package io.igrant.data_wallet.utils

import android.util.Base64.*
import com.mediaparkpk.base58android.Base58
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.connectionRequest.Authentication
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.connectionRequest.Service
import io.igrant.data_wallet.models.connectionRequest.VerificationMethod
import io.igrant.data_wallet.models.decorators.SignatureDecorator
import org.hyperledger.indy.sdk.crypto.Crypto
import java.nio.charset.Charset
import java.util.*
import okhttp3.internal.and


object WalletUtils {

    fun convertDidSovToMyDid(didSov: String): String {
        // 1. base58 decode the did:sov identifier and obtain the public key bytes.
        val decodedSov = Base58.decode(didSov)
        // 2. Add \xed\x01 multicodec prefix to the public key bytes.
        val startArray = byteArrayOf(0xed.toByte(), 0x01)
        val newArray = startArray + decodedSov
        // 3. base58 encode the prefixed public key bytes.
        var encoded = Base58.encode(newArray)
        // 4. prefix the output with ‘z’
        encoded = "z$encoded"
        return encoded
    }

    fun convertDidSovToMyDidWithMyData(didSov: String): String {
        return "did:mydata:${convertDidSovToMyDid(didSov)}"
    }

    fun convertMyDidToDidSov( data: String): String {

        // 0. remove did:mydata
        val myDidRemovedData = data.replace("did:mydata:","")
        // 1. prefix the output with ‘z’
        val myDid = myDidRemovedData.drop(1)

        // 2. base58 decode
        var encoded = Base58.decode(myDid)
        encoded = Arrays.copyOfRange(encoded, 2, encoded.size)

        // 3. return base58 encode the did:sov identifier and obtain the public key bytes.
        return Base58.encode(encoded)
    }

    fun createSignatureDecorator(value: String, signerVk: String): SignatureDecorator {
        // 1. value = payload to be signed (did doc)
        // we are getting it in the parameter

        Logger.d("TAG", "anchorRegistry: 1 : $value")
        // 2. etime = current time in epoch seconds (UTC timestamp)
        val millis = System.currentTimeMillis()
        val seconds = (millis / 1000)

        // 3. etime = convert etime to 8 byte, big-endian encoded string.
        val eTimeEn = String(packRawU64b(1644902745)).replace("\\xbd", "")
        val va = String(value.toByteArray(), Charset.forName("US-ASCII"))
        // 4. combined_value = prefix ascii encoded value with etime

        var combinedVal = eTimeEn + va
        combinedVal = combinedVal.replace("\n", "")
        // 5. signature = indy SDK `sign_message(combined_value, verkey (corresponding to the did:mydata identifier))
        val signature = Crypto.cryptoSign(
            WalletManager.getWallet,
            signerVk,
            combinedVal.toByteArray()
        ).get()

        // 6. esignature = encode signature
        //
        //      base64 URL safe encode signature
        //      convert bytes to ascii string
        //      strip = padding characters from right end of the ascii string
        val eSignature = convertBase64ToAsciiString(signature)

        // 7. ecombined_value = encode combined_value
        //
        //        base64 URL safe encode combined_value
        //        convert bytes to ascii string
        //        strip = padding characters from right end of the ascii string
        val eCombinedValue = convertBase64ToAsciiString(combinedVal.toByteArray())

        return SignatureDecorator(
            "${DidCommPrefixUtils.getType()}/signature/1.0/ed25519Sha512_single",
            eSignature.replace("\n", "").replace("\\u003d\\u003d", ""),
            eCombinedValue.replace("\n", "").replace("\\u003d\\u003d", ""),
            signerVk
        )
    }

    fun toBigEndianBytes(x: Long): ByteArray? {

        var l = x
        val bytes = ByteArray(8)
        val length = bytes.size
        for (i in 0 until length) {
            bytes[length - i - 1] = (l and 0xFF and 0xBD).toByte()
            l = l shr 8
        }

//        Logger.d("TAG", "anchorRegistry: long: $x")
//        val arr = ByteArray(8)
//        for (i in arr.indices) {
//
//            arr[i] = ((x ushr 64 - (i + 1) * 8) and 0xff).toByte()
//            Logger.d("TAG", "anchorRegistry: ${Integer.toHexString(arr[i].toInt())}")
//        }
        return bytes
    }

    private fun packRawU64b(value: Long): ByteArray {
        Logger.d("packRawU64b", "long value : ${value.toInt()}")
        var value = value
        var bx = ByteArray(8)
        if (value >= 0) {
            bx[0] = (value and 0xff).toByte()
            bx[1] = (value shr 8 and 0xff).toByte()
            bx[2] = (value shr 16 and 0xff).toByte()
            bx[3] = (value shr 24 and 0xff).toByte()
            bx[4] = (value shr 32 and 0xff).toByte()
            bx[5] = (value shr 40 and 0xff).toByte()
            bx[6] = (value shr 48 and 0xff).toByte()
            bx[7] = (value shr 56 and 0xff).toByte()
        }
        bx = reverseBytes(bx)
        return bx
    }

    private fun reverseBytes(b: ByteArray): ByteArray {
        var tmp: Byte
        for (i in 0 until b.size / 2) {
            tmp = b[i]
            b[i] = b[b.size - i - 1]
            b[b.size - i - 1] = tmp
        }
        return b
    }


    //      base64 URL safe encode signature
    //      convert bytes to ascii string
    //      strip = padding characters from right end of the ascii string
    fun convertBase64ToAsciiString(data: ByteArray): String {
        // 1 - Base 64 encode data
        val base = encode(
            data, URL_SAFE
        )

        // 2 - Convert to ASCII
        var ascii = String(base, Charset.forName("US-ASCII"))
        Logger.d("ConvertBase64ToAsciiString", "ascii: $ascii")

        // 3 - Removing padding characters
        var index = 0
        while (index < ascii.length) {
            val c: Char = ascii[index]
            if (c == '\u0000' || c == '\u003d') {
                break
            }
            index++
        }

        ascii = ascii.substring(0, index)
        Logger.d("ConvertBase64ToAsciiString", "Removed padding characters: $ascii")
        return ascii
    }

    fun createDidDocForRegistry(myDid: String, myKey: String): DidDoc {

        val myConvertedKey = convertDidSovToMyDid(myKey)
        //verification method
        val verificationMethod = VerificationMethod(
            "did:mydata:$myConvertedKey#1",
            "Ed25519VerificationKey2018",
            "did:mydata:$myConvertedKey",
            myConvertedKey
        )

        val verificationMethods: ArrayList<VerificationMethod> = ArrayList()
        verificationMethods.add(verificationMethod)

        //authentication
        val authentication = Authentication()
        authentication.type = "Ed25519SignatureAuthentication2018"
        authentication.publicKey = "did:mydata:$myConvertedKey#1"

        val authentications: ArrayList<Authentication> = ArrayList()
        authentications.add(authentication)

        //service
        val recipientsKey: ArrayList<String> = ArrayList()
        recipientsKey.add(myConvertedKey)

        val service = Service()
        service.id = "did:mydata:$myConvertedKey;didcomm"
        service.type = "DIDComm"
        service.priority = 0
        service.recipientKeys = recipientsKey
        service.serviceEndpoint = ApiManager.API_URL

        val services: ArrayList<Service> = ArrayList()
        services.add(service)

        //did doc
        val didDoc = DidDoc()
        didDoc.context = "https://w3id.org/did/v1"
        didDoc.id = "did:mydata:${myConvertedKey}"
        didDoc.authentication = authentications
        didDoc.service = services
        didDoc.verificationMethod = verificationMethods

        Logger.d("TAG", "anchorRegistry did doc: ${WalletManager.getGson.toJson(didDoc)}")
        return didDoc
    }

    fun displayBytesInHex(data: ByteArray) {
        val list: ArrayList<Int> = ArrayList()
        for (b in data) {
            list.add(b and 0xff)
        }
        Logger.d("displayBytesInHex", "final out: ${WalletManager.getGson.toJson(list)}")
    }
}