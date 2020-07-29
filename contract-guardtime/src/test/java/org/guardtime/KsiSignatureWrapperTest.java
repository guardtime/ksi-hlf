package org.guardtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;

import com.guardtime.ksi.unisignature.KSISignature;
import org.example.ledgerapi.State;
// import org.guardtime.KSIContext;

// import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.CsvSource;


public class KsiSignatureWrapperTest {
    private final String ksigBase64 = "iAAE5YgBAF0CBFPbpwIDAQ8DBANu/98DAQMFIQERpwCwyAZsR+y6Be03vBTcrbI4VS2Gxlk0LR1+h7h3LQYBAQcjAiEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQGIAQPYAgRT26cCAwEPAwQDbv/fBSEB0fUFNWEdvsdx96L1Qu/GY6atjCUMjo8sJ26pulXSfaEGAQEHIgEBDQMdAwAHMzYtdGVzdAAAAAAAAAAAAAAAAAAAAAAAAAAHJgEBAQIhAeyX7jHMcb3ITZWCpPiMflykQ87Q5VqelxaLC2iASKduByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACCMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByIBAQYDHQMABXRlc3RBAAAAAAAAAAAAAAAAAAAAAAAAAAAAByYBAQcCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhASVwk89CKMWzkzhwkFbzBSc5PwUjvjaWQK1BzdW6d+IkByIBAQcDHQMAAkdUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACCYBAQcCIQEcolXjqqvxrhe4DIgamBl2q8BTSIl0eXMcpsnYnkEb1QcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgjAiEBp80P7jSUU9gFeLWEFPQcJiPTchxPN431aw53a3LdFEoHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQGSHSy+Twv5NMnDymxKadFTTeZFrih7DcbAU9tJzhXypggjAiEB1G4vssrBl2NPKMosFyGmcIsyfWoPjqwxqO7q6pRyiaMHIwIhAcuhQ6JujLbx9XJjInDf5iadYS0lZPIV6G4TY3lvSuGdiAEApAIEU9unAgMBDwUhAXpVPUrJmQcljhgV7D0VbwfMGwrorLrzyi8CAbzyU1c6BgEBByYBAQ0CIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcmAQEuAiEBIiZVeEZBFPE+RS29ABOv2V7ZGJ/4jzaP1paZsYzEqq0HIwIhAWQYEq2N7RAkCJoBjm/ADZJ2b5qQHT3rMCaW4T86sH5I";
    
    @Test
	void test_newFromBase64() {
        KsiSignatureWrapper ksigw = KsiSignatureWrapper.newFromBase64(ksigBase64, 2, "gt");
        
        assertEquals(ksigw.getBlockNumber(), 2, "Invalid block number.");
        assertEquals(ksigw.getKsiBase64(), ksigBase64, "Invalid base64 returned.");
        
        KSISignature sig = ksigw.getKsi();
        assertNotEquals(sig, null, "Signature must not be null");
	}
    
    @Test
	void test_newFromKSI() {
        KsiSignatureWrapper ksigw = KsiSignatureWrapper.newFromBase64(ksigBase64, 2, "gt");
        KSISignature sig = ksigw.getKsi();
        
        KsiSignatureWrapper ksigw2 = KsiSignatureWrapper.newFromKSI(sig, 4, "gt");
        assertEquals(ksigw2.getBlockNumber(), 4, "Invalid block number.");
        assertEquals(ksigw2.getKsiBase64(), ksigBase64, "Invalid base64 returned.");
	}
    
    @Test
	void test_asState() {
        KsiSignatureWrapper ksigw = KsiSignatureWrapper.newFromBase64(ksigBase64, 2, "gt");
        State s = (State)ksigw;
        
        String[] keyl = s.getSplitKey();
        assertEquals(keyl.length, 2, "Key has not 1 components.");
        assertEquals(keyl[0], "gt", "Key component is wrong.");
        assertEquals(keyl[1], "2", "Key component is wrong.");
    }
    
    @Test
	void test_serializeAndDeserialize() {
        KsiSignatureWrapper ksigw = KsiSignatureWrapper.newFromBase64(ksigBase64, 2, "gt");
        State s = (State)ksigw;
        
        byte[] raw = State.serialize(s);
        String aa = new String(raw, StandardCharsets.UTF_8);
        System.out.println(aa);
        
        KsiSignatureWrapper newKsi = KsiSignatureWrapper.deserialize(raw);
        // String str = new String(raw, StandardCharsets.UTF_8);

        assertEquals(ksigw.getBlockNumber(), newKsi.getBlockNumber(), "Block numbers do not match.");
        assertEquals(ksigw.isExtended(), newKsi.isExtended(), "Is extended flag do not match.");
        assertEquals(ksigw.getKsiBase64(), newKsi.getKsiBase64(), "KSI signature base64 do not match.");
    }
    

}