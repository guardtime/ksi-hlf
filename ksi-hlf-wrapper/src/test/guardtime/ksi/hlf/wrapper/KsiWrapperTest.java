/*
 * Copyright 2021 Guardtime, Inc.
 *
 * This file is part of the KSI-HLF integration toolkit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES, CONDITIONS, OR OTHER LICENSES OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * "Guardtime" and "KSI" are trademarks or registered trademarks of
 * Guardtime, Inc., and no license to trademarks is granted; Guardtime
 * reserves and retains all trademark rights.
 */

package test.guardtime.ksi.hlf.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.io.InvalidObjectException;

import org.guardtime.ksi.hlf.ledgerapi.State;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.unisignature.KSISignature;
import org.guardtime.ksi.hlf.wrapper.KsiWrapper;

public class KsiWrapperTest {
    // private final String ksigBase64 =
    // "iAAE5YgBAF0CBFPbpwIDAQ8DBANu/98DAQMFIQERpwCwyAZsR+y6Be03vBTcrbI4VS2Gxlk0LR1+h7h3LQYBAQcjAiEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQGIAQPYAgRT26cCAwEPAwQDbv/fBSEB0fUFNWEdvsdx96L1Qu/GY6atjCUMjo8sJ26pulXSfaEGAQEHIgEBDQMdAwAHMzYtdGVzdAAAAAAAAAAAAAAAAAAAAAAAAAAHJgEBAQIhAeyX7jHMcb3ITZWCpPiMflykQ87Q5VqelxaLC2iASKduByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACCMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByIBAQYDHQMABXRlc3RBAAAAAAAAAAAAAAAAAAAAAAAAAAAAByYBAQcCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhASVwk89CKMWzkzhwkFbzBSc5PwUjvjaWQK1BzdW6d+IkByIBAQcDHQMAAkdUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACCYBAQcCIQEcolXjqqvxrhe4DIgamBl2q8BTSIl0eXMcpsnYnkEb1QcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgjAiEBp80P7jSUU9gFeLWEFPQcJiPTchxPN431aw53a3LdFEoHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQGSHSy+Twv5NMnDymxKadFTTeZFrih7DcbAU9tJzhXypggjAiEB1G4vssrBl2NPKMosFyGmcIsyfWoPjqwxqO7q6pRyiaMHIwIhAcuhQ6JujLbx9XJjInDf5iadYS0lZPIV6G4TY3lvSuGdiAEApAIEU9unAgMBDwUhAXpVPUrJmQcljhgV7D0VbwfMGwrorLrzyi8CAbzyU1c6BgEBByYBAQ0CIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcmAQEuAiEBIiZVeEZBFPE+RS29ABOv2V7ZGJ/4jzaP1paZsYzEqq0HIwIhAWQYEq2N7RAkCJoBjm/ADZJ2b5qQHT3rMCaW4T86sH5I";
    private final String ksigBase64 = "iAAHwIgBAGoCBF8pT+wDAQsDAT0DARUDARUDAQMFIQFkp+yjRVuGLvpsmuiNe9qHHldB0MTR1Q6T4MH4ikUN3QYBAQctBCh+AQFhBWFub24AYhFrc2lndy10ZXN0dXNlcjoxAGMAZAcFrAwfwhtwAQEBiAEAzQIEXylP7AMBCwMBPQMBFQMBFQUhAW0/G4r+snw8/0edt/UikBQzZgeB0tj+rLkSTOFCu1L4BgEBByQEIn4CAQFhA0dUAGILQUxlMi0xLTI6NgBjAQxkBwWsDB/IOQoIIwIhARcmgulXBEWHoO5i22tGK5nBZhHCmagN3aeUBzfwTDv0ByMCIQGDcV+V9qtUAg0R6qRP34qB1qbJ1AnKynD7CEVRucczsQgjAiEBQrRVncPsPzJdmfMLc/ALRSXBKr1W7JiEY2qBYG7zQjGIAQDIAgRfKU/sAwELAwE9AwEVBSEBRzc1tmeLj/3tmKUKUjbzJ/P8vBM0KzJ9/8GiZWt6yJAGAQEHIgQgfgIBAWEDR1QAYglBU2UyLTA6MQBjAQNkBwWsDB/OIU4IIwIhAatbgNLoyGbJlAC/bBhwIKx4AyEQF5Zh2ALg5N5L3/4dByMCIQG73G4b+l5GSS/E/fKnvROy0D6SzfuJb2CKeREskcASrwgjAiEBhD7i2FYRMXUbvg2awpVfw8OsLR8RGTm2L3rYvHkk6rKIAQDoAgRfKU/sAwELAwE9BSEB5Qo0KYrWKTHuy7gy5SwWjuWcJ7x70+n92bYSPwujiuoGAQEHIAQefgIBAWEDR1QAYgdBTmUyOjAAYwEUZAcFrAwfzko/CCMCIQGrWt3uuAoHcyWBYN4Ew+Rw5jrfNMWJoZmw6uxJd+jzBAcjAiEBCrmOQpvW9UO8ajWixWdFKgn8cWBe1AyoUshQzvw/0FQHIwIhAda0cdvsSqo3mkHn2UMeGgv7iGZDP6PvlR7wE8S/JH6VByMCIQH6VdQPcfbHc33osXjshXOzgVxgaOrkmoxCUgWDuRZz6IgBAKQCBF8pT+wDAQsFIQH3KIjmu927BdRJuIKxxZfYds+NvIH7DDYkqyHN2/nkEgYBAQcmAQFDAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHJgEBLAIhAWc/QvVugKjr0O67DHx/vMC8yNBVdb+OY/hgVKeD2p5hCCMCIQE0M8FtBpk8gEHZicTW3uwMr2DorHna79LgWfotiT3ls4gCAsgBBF8pT+wCBF8pT+wFIQG5KedLqKU9gEQcy0ZYGEPMYF4fKXGNHoLE5KTZY2qLiAghAQoS+KEuqU/SKziPNx3FX8N8oJYvRsQUiKiPRTzvF43fCCEBepi0XC4rMqLmELHI+6gKwJsBzW0iRuzX8Le6ixdi9q4IIQFDlfTI8sLLYmG/uZI8cwr66wFEG3rT8CNsasPAqkj1JwghAUUHvtZz5GKMI3UtWRbG02VjnK7HIeJBGtrnLSf5IMSSCCEBfrvWPwyL0At0Sue6rJOgyh8Av/LRWQ74xF2bqR6AEjoIIQE0e+FvYNZTU5+OFmN0ztbgy8g47utHpcFcMVp0p0Ky9wghAdXZgUw5moj3t5aa6los3PUeMHtxzfk4ncqg48SRrOHNCCEBmHqtMA1BavYtBcdcEc4Fv8+4JqpXb8UoKKBHSEmwnnYIIQFStbJdncQ/zV5v6qKp3VCnN8B49lEFQDNVmHyBfJiMmAghAUYQM3DRB0wGajpbfELbwFcLCMkQE1JBtESpFw4CShriCCEBjusUMgTRFGsBUej32D99wU95tHl0fhCcl3BswfsNpDkIIQFULCLnFjlnIK51Az+bUblsq39NHinbfGisHfkA8Zl60QghAdtSQQq0Fk5j1rumhqAqQk8N4IqHCBYCPK2Lzs/w877jCCEBh0MvTB9iwogSPqoP+10U4N1rGD98R0Ei+WwMUJjK8O0IIQGrPxknAH2+n3wYmrsPDj3cHQJlePcLtvuYDh24evfIFQghAVa3tzIQKtuhtdjcQgJLxyisoBQvNWi562VHUHmGtlDXCCEB68OrHYZkFYETCsPHB3txtnu6TJFVMOn+SbmHafyNyusIIQFJb8ASDYVOdTS5kqsy7DBFsg1L7hv75FZP0JLOr6CLcgghAbtE/Tal883ue1xt86YJignjUzNbYCnxR3UCWIp+N74AiAUBUTApAgRfKU/sBCEBr5/U1g47Qw23oFl0UUQwxxT1CNOE3sB7dAr5uVn+2cSACwEiARYxLjIuODQwLjExMzU0OS4xLjEuMTEAgAIBALMAfO5ZZi8cjqsRgZ0dBzPhJzB3JEvUZfPodSSOWs67fuKeiliCd//Q1LDSNOuUOjCKCNHwcL/N6m+HAG+dIUY6d4RGg1pCgnrRDQQrxG0FRAP1s6silWIrtRLp4L5fQg2MsP4EjgyvDoj+VtN4BuW47dFvra2FmAq9+4nc+NaQj7SnuQctflBysND3LExT3xaWWSh0DaDLqbbXhlvdj4mW//BFZgkBovFIx8Qiuo5Zr0tOZXnMCj8fFic//V5Wr0MUO+NJEsay71xim8n8mGqWjqdrEsFhSIs1tBsoEHHtpkv1aI/K1u9wmK6hSUhylxtV5QDbu+9AonILcJU+3I8DBAWhtjA=";
    private final String headerHash = "AfzCZlb7afb5scuNMll1xAi02+dlhKyh7krToQdoDlHW";
    private final String metaHash = "AWZ/uO7IVboDuflbBBBUvrRpKCIZNyaAnCWOKgmh9wUm";
    private final String inHash = "AWSn7KNFW4Yu+mya6I172oceV0HQxNHVDpPgwfiKRQ3d";
    private final String[] rechash = { headerHash, metaHash };

    @Test
    void test_newFromBase64() throws IllegalArgumentException, KSIException {
        KsiWrapper ksigw = KsiWrapper.newFromBase64(ksigBase64, rechash, 2, "gt");

        assertEquals(ksigw.getBlockNumber(), 2, "Invalid block number.");
        assertEquals(ksigw.getKsiBase64(), ksigBase64, "Invalid base64 returned.");

        KSISignature sig = ksigw.getKsi();
        assertNotEquals(sig, null, "Signature must not be null");
    }

    @Test
    void test_newFromKSI() throws IllegalArgumentException, KSIException {
        KsiWrapper ksigw = KsiWrapper.newFromBase64(ksigBase64, rechash, 2, "gt");
        KSISignature sig = ksigw.getKsi();

        KsiWrapper ksigw2;
        ksigw2 = KsiWrapper.newFromKSI(sig, ksigw.getRecordHash(), 4, "gt");
        assertEquals(ksigw2.getBlockNumber(), 4, "Invalid block number.");
        assertEquals(ksigw2.getKsiBase64(), ksigBase64, "Invalid base64 returned.");
	}
    
    @Test
	void test_asState() throws IllegalArgumentException, KSIException {
        KsiWrapper ksigw = KsiWrapper.newFromBase64(ksigBase64, rechash, 2, "gt");
        State s = (State)ksigw;
        
        String[] keyl = s.getSplitKey();
        assertEquals(keyl.length, 2, "Key has not 1 components.");
        assertEquals(keyl[0], "gt", "Key component is wrong.");
        assertEquals(keyl[1], "2", "Key component is wrong.");
    }
    
    @Test
	void test_serializeAndDeserialize() throws Exception {
        KsiWrapper ksigw = KsiWrapper.newFromBase64(ksigBase64, rechash, 2, "gt");
        State s = (State)ksigw;
        
        byte[] raw = s.serialize();
        String aa = new String(raw, StandardCharsets.UTF_8);
        System.out.println(aa);
        
        KsiWrapper newKsi = new KsiWrapper().parse(raw);
        // String str = new String(raw, StandardCharsets.UTF_8);

        assertEquals(ksigw.getBlockNumber(), newKsi.getBlockNumber(), "Block numbers do not match.");
        assertEquals(ksigw.isExtended(), newKsi.isExtended(), "Is extended flag do not match.");
        assertEquals(ksigw.getKsiBase64(), newKsi.getKsiBase64(), "KSI signature base64 do not match.");
    }
}