package org.guardtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertSelector;
import java.util.List;
import com.guardtime.ksi.Extender;
import com.guardtime.ksi.ExtenderBuilder;
import com.guardtime.ksi.PublicationsHandler;
import com.guardtime.ksi.PublicationsHandlerBuilder;
import com.guardtime.ksi.Signer;
import com.guardtime.ksi.SignerBuilder;
import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.service.KSIExtendingClientServiceAdapter;
import com.guardtime.ksi.service.KSISigningClientServiceAdapter;
import com.guardtime.ksi.service.client.KSIExtenderClient;
import com.guardtime.ksi.service.client.KSIPublicationsFileClient;
import com.guardtime.ksi.service.client.KSIServiceCredentials;
import com.guardtime.ksi.service.client.KSISigningClient;
import com.guardtime.ksi.service.client.ServiceCredentials;
import com.guardtime.ksi.service.http.simple.SimpleHttpSigningClient;
import com.guardtime.ksi.trust.X509CertificateSubjectRdnSelector;
import com.guardtime.ksi.service.client.http.CredentialsAwareHttpSettings;
import com.guardtime.ksi.service.client.http.HttpSettings;
import com.guardtime.ksi.service.http.simple.SimpleHttpExtenderClient;
import com.guardtime.ksi.service.http.simple.SimpleHttpPublicationsFileClient;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.yaml.snakeyaml.Yaml;

/**
 * Conf
 */
public class Conf {

    public Conf() {
    
    }

    private String user;
    private String network;
    private String walletPath;
    private String connectionProfile;
    
    private String aggrUrl;
    private String aggrUser;
    private String aggrKey;
    
    private String extUrl;
    private String extUser;
    private String extKey;
    
    private String pubfileUrl;
    private String pubfileConstraint;
    private String pubfileCert;
    private boolean disabled;

    private String commitOrg;

    public boolean isDisabled() {
        return this.disabled;
    }

    public boolean getDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }


    public void setGateway(String user, String network, String wallet, String connectionProfile) {
        this.setUser(user);
        this.setNetwork(network);
        this.setWalletPath(wallet);
        this.setConnectionProfile(connectionProfile);
    }

    public void setAggregator(String url, String user, String key) {
        this.setAggrUrl(url);
        this.setAggrUser(user);
        this.setAggrKey(key);
    }
    
    public void setExtender(String url, String user, String key) {
        this.setExtUrl(url);
        this.setExtUser(user);
        this.setExtKey(key);
    }

    public void setPubfile(String url, String constraint) {
        this.setPubfileUrl(url);
        this.setPubfileConstraint(constraint);
    }

    static List<Conf> ConfFromFile(String path) throws IOException {
        File infile = new File(path);

        try (FileInputStream in = new FileInputStream(infile)) {
            List<Conf> tmp = new Yaml().load(in);
            
            for (int i = 0; i < tmp.size(); i++) {
                tmp.get(i).validate(i);
            }

            return tmp;
        } catch (Exception e) {
          throw e;  
        }
    }
    
    private void validate(int confNr) {
        if (this.aggrUrl == null || this.aggrUrl.isEmpty()) throw new IllegalArgumentException("aggrUrl is empty or null in conf nr. " + confNr);
        if (this.aggrKey == null || this.aggrKey.isEmpty()) throw new IllegalArgumentException("aggrKey is empty or null in conf nr. " + confNr);
        if (this.aggrUser == null || this.aggrUser.isEmpty()) throw new IllegalArgumentException("aggrUser is empty or null in conf nr. " + confNr);
        
        if (this.extUrl == null || this.extUrl.isEmpty()) throw new IllegalArgumentException("extUrl is empty or null in conf nr. " + confNr);
        if (this.extKey == null || this.extKey.isEmpty()) throw new IllegalArgumentException("extKey is empty or null in conf nr. " + confNr);
        if (this.extUser == null || this.extUser.isEmpty()) throw new IllegalArgumentException("extUser is empty or null in conf nr. " + confNr);
        
        if (this.pubfileUrl == null || this.pubfileUrl.isEmpty()) throw new IllegalArgumentException("pubfileUrl is empty or null in conf nr. " + confNr);
        if (this.pubfileConstraint == null || this.pubfileConstraint.isEmpty()) throw new IllegalArgumentException("pubfileConstraint is empty or null in conf nr. " + confNr);
        
        if (this.connectionProfile == null || this.connectionProfile.toString().isEmpty()) throw new IllegalArgumentException("connectionProfile is empty or null in conf nr. " + confNr);
        if (this.walletPath == null || this.walletPath.toString().isEmpty()) throw new IllegalArgumentException("walletPath is empty or null in conf nr. " + confNr);
        if (this.network == null || this.network.isEmpty()) throw new IllegalArgumentException("network is empty or null in conf nr. " + confNr);
        if (this.user == null || this.user.isEmpty()) throw new IllegalArgumentException("user is empty or null in conf nr. " + confNr);
    }

    // static void ConfToFile(List<Conf> conf, String path) {
    //     File outFile = new File(path);
    //     Yaml yaml = new Yaml();
    //     // StringWriter sw = new StringWriter();
    //     // System.out.println("========");
    //     // System.out.println(yaml.dump(conf));
    //     // System.out.println("========");

    //     try {
    //         FileOutputStream out = new FileOutputStream(outFile);
    //         out.write(yaml.dump(conf).getBytes());
    //         out.close();
    //     } catch (Exception e) {
    //         //TODO: handle exception
    //     }
    // }


    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPubfileCert() {
        return this.pubfileCert;
    }

    public void setPubfileCert(String pubfileCert) {
        this.pubfileCert = pubfileCert;
    }

    public String getNetwork() {
        return this.network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getWalletPath() {
        return this.walletPath.toString();
    }

    public void setWalletPath(String walletPath) {
        this.walletPath = walletPath;
    }

    public String getConnectionProfile() {
        return this.connectionProfile;
    }

    public void setConnectionProfile(String connectionProfile) {
        this.connectionProfile = connectionProfile;
    }

    public String getCommitOrg() {
        return this.commitOrg;
    }

    public void setCommitOrg(String commitOrg) {
        this.commitOrg = commitOrg;
    }

    public String getAggrUrl() {
        return this.aggrUrl;
    }

    public void setAggrUrl(String aggrUrl) {
        this.aggrUrl = aggrUrl;
    }

    public String getAggrUser() {
        return this.aggrUser;
    }

    public void setAggrUser(String aggrUser) {
        this.aggrUser = aggrUser;
    }

    public String getAggrKey() {
        return this.aggrKey;
    }

    public void setAggrKey(String aggrKey) {
        this.aggrKey = aggrKey;
    }

    public String getExtUrl() {
        return this.extUrl;
    }

    public void setExtUrl(String extUrl) {
        this.extUrl = extUrl;
    }

    public String getExtUser() {
        return this.extUser;
    }

    public void setExtUser(String extUser) {
        this.extUser = extUser;
    }

    public String getExtKey() {
        return this.extKey;
    }

    public void setExtKey(String extKey) {
        this.extKey = extKey;
    }

    public String getPubfileUrl() {
        return this.pubfileUrl;
    }

    public void setPubfileUrl(String pubfileUrl) {
        this.pubfileUrl = pubfileUrl;
    }

    public String getPubfileConstraint() {
        return this.pubfileConstraint;
    }

    public void setPubfileConstraint(String pubfileConstraint) {
        this.pubfileConstraint = pubfileConstraint;
    }

    public Gateway getGateway() throws IOException {
        Path walletPath = Paths.get(this.getWalletPath());
        Path connectionProfile = Paths.get(this.getConnectionProfile());
        Gateway.Builder builder = Gateway.createBuilder();
        
        

        Wallet wallet = Wallets.newFileSystemWallet(walletPath);
        
        // Set connection options on the gateway builder
        
        builder.identity(wallet, this.getUser()).networkConfig(connectionProfile).discovery(true);
        
        // System.out.println("  Connecting..");
        return builder.connect();
    }

    private KSISigningClient getSigningClient() {
        String key = this.getAggrKey();
        String user = this.getAggrUser();
        String url = this.getAggrUrl();
    
        ServiceCredentials credentials = new KSIServiceCredentials(user, key);
        KSISigningClient ksiSigningClient = new SimpleHttpSigningClient(new CredentialsAwareHttpSettings(url, credentials));
     
        return ksiSigningClient;
    }
    
    private KSIExtenderClient getExtendingClient() {
        String key = this.getExtKey();
        String user = this.getExtUser();
        String url = this.getExtUrl();
    
        ServiceCredentials credentials = new KSIServiceCredentials(user, key);
        KSIExtenderClient ksiExtenderClient = new SimpleHttpExtenderClient(new CredentialsAwareHttpSettings(url, credentials));
     
        return ksiExtenderClient;
    }

    public PublicationsHandler getPubHandler() throws KSIException {
        String url = this.getPubfileUrl();
        CertSelector certSelector = new X509CertificateSubjectRdnSelector(this.getPubfileConstraint());
        KSIPublicationsFileClient ksiPublicationsFileClient = new SimpleHttpPublicationsFileClient(new HttpSettings(url));
        
        PublicationsHandlerBuilder bldr = new PublicationsHandlerBuilder();
        bldr.setKsiProtocolPublicationsFileClient(ksiPublicationsFileClient);
        bldr.setPublicationsFileCertificateConstraints(certSelector);

        if (this.pubfileCert != null && !this.pubfileCert.isEmpty()) {
            bldr.setPublicationsFilePkiTrustStore(new File(this.pubfileCert), "aaaaaa");
        }

        PublicationsHandler publicationsHandler = bldr.build();
        
        return publicationsHandler;
    }

    public Signer getSigner() {
        KSISigningClient ksiSigningClient = this.getSigningClient();
        Signer signer = new SignerBuilder().setSigningService(new KSISigningClientServiceAdapter(ksiSigningClient)).build();
        return signer;
    }

    public Extender getExtender() throws KSIException {
        KSIExtenderClient ksiExtenderClient = this.getExtendingClient();
        PublicationsHandler pubHandler = this.getPubHandler();
    
        Extender extender = new ExtenderBuilder()
            .setExtendingService(new KSIExtendingClientServiceAdapter(ksiExtenderClient)).setPublicationsHandler(pubHandler)
            .build();
        return extender;
      }

    @Override
    public String toString() {
        return  "{\n" +
                "  aggrUrl= '" + getAggrUrl() + "'\n" +
                "  aggrUser='" + getAggrUser() + "'\n" +
                "  aggrKey= '" + getAggrKey() + "'\n" +
                    
                "  extUrl=  '" + getExtUrl() + "'\n" +
                "  extUser= '" + getExtUser() + "'\n" +
                "  extKey=  '" + getExtKey() + "'\n" +
                
                "  pubfileCert=      '" + getPubfileCert() + "'\n" +
                "  pubfileUrl=       '" + getPubfileUrl() + "'\n" +
                "  pubfileConstraint='" + getPubfileConstraint() + "'\n" +
                
                "  network=          '" + getNetwork() + "'\n" +
                "  user=             '" + getUser() + "'\n" +
                "  walletPath=       '" + getWalletPath() + "'\n" +
                "  connectionProfile='" + getConnectionProfile() + "'\n" +
                "  commitOrg=        '" + getCommitOrg() + "'\n" +
                
                "  disabled= '" + isDisabled() + "'\n" +
                "}";
    }
 }