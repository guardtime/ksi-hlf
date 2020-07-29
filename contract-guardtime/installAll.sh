projectRoot="/root/go/fabric-samples/commercial-paper/organization/"

set -e

echo "INSTALLING ON MAGNETOCORP"
cd "$projectRoot/magnetocorp"
source magnetocorp.sh
source "$projectRoot/magnetocorp/contract-guardtime/installLocal.sh"

echo "File name:" $fname
echo "Package ID:" $PACKAGE_ID


echo ""
echo ""
echo ""
echo ""
echo "INSTALLING ON DIGIBANK"
cd "$projectRoot/digibank"
cp "$projectRoot/magnetocorp/$fname" "$projectRoot/digibank/"
source digibank.sh
source "$projectRoot/magnetocorp/contract-guardtime/installLocal.sh"

#echo "VAATA SEDA:" $fname
#echo "VAATA SEDA:" $PACKAGE_ID

 echo "TODO: Commit chaincode "
 peer lifecycle chaincode commit -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --peerAddresses localhost:7051 --tlsRootCertFiles ${PEER0_ORG1_CA} --peerAddresses localhost:9051 --tlsRootCertFiles ${PEER0_ORG2_CA} --channelID mychannel --name $contractname -v $version --sequence $newsqnr --tls --cafile $ORDERER_CA --waitForEvent
 peer lifecycle chaincode querycommitted --channelID mychannel