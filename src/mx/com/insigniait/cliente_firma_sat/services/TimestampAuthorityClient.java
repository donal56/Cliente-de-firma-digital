package mx.com.insigniait.cliente_firma_sat.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.io.IOUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.Attributes;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;

/*
 * Cliente de firma de estampas de tiempo
 */
public class TimestampAuthorityClient {
	
    private URL 			url			=	null;
    private String 			username	=	null;
    private String 			password	=	null;
    private MessageDigest 	digest		=	null;

    /*
     * Datos de la autoridad de estampa de tiempo (TSA)
     */
    public TimestampAuthorityClient(String url, String username, String password) throws MalformedURLException, NoSuchAlgorithmException {
        this.url 		= 	new URL(url);
        this.username 	= 	username;
        this.password 	= 	password;
        this.digest 	= 	MessageDigest.getInstance("SHA-256");
    }

    public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public MessageDigest getDigest() {
		return digest;
	}

	public void setDigest(MessageDigest digest) {
		this.digest = digest;
	}
	
    /**
     * Extend cms signed data with TimeStamp first or to all signers
     */
    public CMSSignedData addSignedTimeStamp(CMSSignedData signedData) throws IOException, TSPException, NoSuchAlgorithmException {
    	
        SignerInformationStore signerStore = signedData.getSignerInfos();
        List<SignerInformation> signersWithTimeStamp = new ArrayList<>();

        // This adds a timestamp to every signer (into his unsigned attributes) in the signature.
        for (SignerInformation signer : signerStore.getSigners()) {
            signersWithTimeStamp.add(signTimeStamp(signer));
        }

        // new SignerInformationStore have to be created cause new SignerInformation instance
        // also SignerInformationStore have to be replaced in a signedData
        return CMSSignedData.replaceSigners(signedData, new SignerInformationStore(signersWithTimeStamp));
    }

    /**
     * Extend CMS Signer Information with the TimeStampToken into the unsigned Attributes.
     */
    private SignerInformation signTimeStamp(SignerInformation signer) throws IOException, TSPException {
        AttributeTable unsignedAttributes = signer.getUnsignedAttributes();

        ASN1EncodableVector vector = new ASN1EncodableVector();
        if (unsignedAttributes != null) {
            vector = unsignedAttributes.toASN1EncodableVector();
        }

        byte[] token = getTimeStampToken(signer.getSignature());
        ASN1ObjectIdentifier oid = PKCSObjectIdentifiers.id_aa_signatureTimeStampToken;
        ASN1Encodable signatureTimeStamp = new Attribute(oid, new DERSet(ASN1Primitive.fromByteArray(token)));
        vector.add(signatureTimeStamp);
        
        Attributes signedAttributes = new Attributes(vector);

        // replace unsignedAttributes with the signed once
        return SignerInformation.replaceUnsignedAttributes(signer, new AttributeTable(signedAttributes));
    }

	/*
	 * Recuperar estampa de tiempo
     */
    private byte[] getTimeStampToken(byte[] messageImprint) throws IOException, TSPException {
        this.digest.reset();
        byte[] hash = this.digest.digest(messageImprint);

        // generate cryptographic nonce
        SecureRandom random = new SecureRandom();
        int nonce = random.nextInt();

        // generate TSA request
        TimeStampRequestGenerator tsaGenerator = new TimeStampRequestGenerator();
        tsaGenerator.setCertReq(true);
        ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha256.getId());
        TimeStampRequest request = tsaGenerator.generate(oid, hash, BigInteger.valueOf(nonce));

        // get TSA response
        byte[] tsaResponse = getTSAResponse(request.getEncoded());

        TimeStampResponse response = new TimeStampResponse(tsaResponse);
        response.validate(request);

        TimeStampToken token = response.getTimeStampToken();
        if (token == null) {
            throw new IOException("Incapaz de comunicarse con el servicio de autenticación de estampas de tiempo.");
        }

        return token.getEncoded();
    }

    /*
     * Recuperar respuesta de TSA
     */
    private byte[] getTSAResponse(byte[] request) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "application/timestamp-query");

        if (this.username != null && this.password != null && !this.username.trim().equals("") && !this.password.trim().equals("")) {
            connection.setRequestProperty(this.username, this.password);
        }

        //Solicitud
        OutputStream output = null;
        try {
            output = connection.getOutputStream();
            output.write(request);
        } 
        finally {
            IOUtils.closeQuietly(output);
        }
        
        //Respuesta
        InputStream input = null;
        byte[] response;
        try {
            input = connection.getInputStream();
            response = IOUtils.toByteArray(input);
        } 
        finally {
            IOUtils.closeQuietly(input);
        }

        return response;
    }
}