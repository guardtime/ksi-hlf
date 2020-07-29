#!/bin/bash

set -e

contractname="ksionblock"
package="gt$contractname"

version="0"
contractpath="./contract-guardtime"

sqnr=$(peer lifecycle chaincode querycommitted --channelID mychannel | grep "$contractname" | awk '{print $5$6}' | tr -dc '0-9')
newsqnr=$((${sqnr} + 1))

fname="gtksionblock"$newsqnr".tar.gz"
label="gt_"$newsqnr"_"$version


echo "Sequence is    " $sqnr
echo "New Sequence is" $newsqnr
echo "New File name  " $fname
echo "New Label      " $label
echo "MSPID" $CORE_PEER_LOCALMSPID
echo "PEER" $CORE_PEER_ADDRESS

if test -f "$fname"; then
    echo "  No need to package $fname exists."
else
    echo "  Packaging: $fname $label"
    peer lifecycle chaincode package $fname --lang java --path $contractpath --label $label
fi

installedStr=$(peer lifecycle chaincode queryinstalled | grep $label || true)

if [ "$installedStr" = "" ]; then
    echo "  Installing: $fname $label"
    peer lifecycle chaincode install $fname

    PACKAGE_ID=$(peer lifecycle chaincode queryinstalled | grep -o $label:[a-f0-9]* || true)  
    echo "PACKAGE_ID       " $PACKAGE_ID

    echo "Prove chaincode : $contractname $newsqnr $PACKAGE_ID"
    peer lifecycle chaincode approveformyorg --orderer localhost:7050 --ordererTLSHostnameOverride orderer.example.com --channelID mychannel --name $contractname -v $version --package-id $PACKAGE_ID --sequence $newsqnr --tls --cafile $ORDERER_CA
else
    echo "  Is already installed:" $installedStr
fi






 
