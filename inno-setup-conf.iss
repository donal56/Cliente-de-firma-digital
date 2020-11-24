; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "Cliente de firma digital SAT"
#define MyAppVersion "1.0"
#define MyAppPublisher "InsigniaIT"
#define MyAppURL "insigniait.com.mx"
#define MyAppExeName "Cliente de firma digital SAT.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application. Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{99361B52-F9A2-4D80-B098-AAE19999DCEF}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\InsigniaIT\Cliente de firma digital SAT
DisableProgramGroupPage=yes
; Uncomment the following line to run in non administrative install mode (install for current user only.)
;PrivilegesRequired=lowest
OutputBaseFilename=installer
Compression=lzma
SolidCompression=yes
WizardStyle=modern  
PrivilegesRequired=admin

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "C:\Users\donal\Documents\Eclipse EE\Cliente de firma digital SAT\binaries\1.0\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\donal\Documents\Eclipse EE\Cliente de firma digital SAT\binaries\1.0\icono.ico"; DestDir: "{app}"; Flags: ignoreversion
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

;[Run]
;Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent runascurrentuser

[Registry]
Root: HKCR; Subkey: "cfd"; Flags: createvalueifdoesntexist uninsdeletekey
Root: HKCR; Subkey: "cfd"; ValueType: string; ValueName: "URL protocol"; ValueData: ""; Flags: createvalueifdoesntexist uninsdeletekey  
Root: HKCR; Subkey: "cfd\DefaultIcon"; Flags: createvalueifdoesntexist uninsdeletekey   
Root: HKCR; Subkey: "cfd\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}, 1"""; Flags: createvalueifdoesntexist uninsdeletekey
Root: HKCR; Subkey: "cfd\shell"; Flags: createvalueifdoesntexist uninsdeletekey
Root: HKCR; Subkey: "cfd\shell\open"; Flags: createvalueifdoesntexist uninsdeletekey
Root: HKCR; Subkey: "cfd\shell\open\command"; Flags: createvalueifdoesntexist uninsdeletekey
Root: HKCR; Subkey: "cfd\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; Flags: createvalueifdoesntexist uninsdeletekey
