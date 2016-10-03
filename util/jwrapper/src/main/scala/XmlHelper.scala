object XmlHelper {
  private[this] val badJars = Seq("scalac-scapegoat-plugin", "webjars")

  def xml(dir: String) = {
    val libFiles = new java.io.File(dir + "/target/universal/stage/lib").list().filterNot(x => badJars.exists(x.contains))
    <jwrapper>
      <BundleName>Database Flow</BundleName>
      <SplashPNG>{ dir }/util/jwrapper/src/main/resources/JWrapper-Database Flow-splash.png</SplashPNG>
      <BundleLogoPNG>{ dir }/public/images/ui/favicon/favicon-512.png</BundleLogoPNG>
      <InstallType>NoInstall</InstallType>
      <App>
        <Name>Database Flow</Name>
        <LogoPNG>{ dir }/public/images/ui/favicon/favicon-512.png</LogoPNG>
        <MainClass>DatabaseFlow</MainClass>
      </App>
      <SupportedLanguages>en</SupportedLanguages>
      <Windows32JRE>{ dir }/tmp/jwrapper/jre-installs/win32/jre1.8.0_101</Windows32JRE>
      <Windows64JRE>{ dir }/tmp/jwrapper/jre-installs/win64/jre1.8.0_101</Windows64JRE>
      <Linux32JRE>{ dir }/tmp/jwrapper/jre-installs/linux/jre1.8.0_101</Linux32JRE>
      <Linux64JRE>{ dir }/tmp/jwrapper/jre-installs/linuxx64/jre1.8.0_101</Linux64JRE>
      <Mac64JRE>{ dir }/tmp/jwrapper/jre-installs/macos64/jre1.8.0_101</Mac64JRE>
      <WindowsElevation>None</WindowsElevation>
      <WindowsElevationUiAccess>true</WindowsElevationUiAccess>
      { for (f <- libFiles) yield <File classpath='yes'>{ dir + "/target/universal/stage/lib/" + f }</File> }
      <JvmOptions>
        <JvmOption>-Xmx1024m</JvmOption>
        <JvmOption>-Dshow.gui=true</JvmOption>
      </JvmOptions>
      <!-- IMPORTANT place custom content BELOW this comment for compatibility with JW App -->
      <!--
      <SignForMac>
        <AppleRootCertificate>{ dir }/build/jwrapper/certs/AppleIncRootCertificate.cer</AppleRootCertificate>
        <DeveloperIdCertificate>{ dir }/build/jwrapper/certs/DeveloperIDCA.cer</DeveloperIdCertificate>
        <DeveloperIdP12>{ dir }/build/jwrapper/certs/certificates.p12</DeveloperIdP12>
        <DeveloperIdAlias>mac app submission: kyle unverferth</DeveloperIdAlias>
        <DeveloperIdPassword>dbflow</DeveloperIdPassword>
      </SignForMac>
      -->
    </jwrapper>
  }
}
