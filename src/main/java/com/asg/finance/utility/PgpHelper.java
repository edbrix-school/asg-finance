package com.asg.finance.utility;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;
import org.springframework.stereotype.Component;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

@Component
public class PgpHelper {

    public PgpHelper() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public PGPSecretKey readSecretKeyFromFile(String path) throws Exception {
        try (InputStream in = new FileInputStream(path)) {
            return readSecretKey(PGPUtil.getDecoderStream(in));
        }
    }

    public PGPPublicKey readPublicKeyFromFile(String path) throws Exception {
        try (InputStream in = new FileInputStream(path)) {
            return readPublicKey(PGPUtil.getDecoderStream(in));
        }
    }

    public PGPSecretKey readSecretKey(InputStream input) throws Exception {
        InputStream decoderStream = PGPUtil.getDecoderStream(input);
        PGPSecretKeyRingCollection keyRings = new PGPSecretKeyRingCollection(decoderStream, new JcaKeyFingerprintCalculator());
        Iterator<PGPSecretKeyRing> rings = keyRings.getKeyRings();
        while (rings.hasNext()) {
            PGPSecretKeyRing ring = rings.next();
            Iterator<PGPSecretKey> keys = ring.getSecretKeys();
            while (keys.hasNext()) {
                PGPSecretKey key = keys.next();
                if (key.isSigningKey()) return key;
            }
        }
        throw new IllegalArgumentException("No signing key found");
    }

    public PGPPublicKey readPublicKey(InputStream input) throws Exception {
        InputStream decoderStream = PGPUtil.getDecoderStream(input);
        PGPPublicKeyRingCollection keyRings = new PGPPublicKeyRingCollection(decoderStream, new JcaKeyFingerprintCalculator());
        Iterator<PGPPublicKeyRing> rings = keyRings.getKeyRings();
        while (rings.hasNext()) {
            PGPPublicKeyRing ring = rings.next();
            Iterator<PGPPublicKey> keys = ring.getPublicKeys();
            while (keys.hasNext()) {
                PGPPublicKey key = keys.next();
                if (key.isEncryptionKey()) return key;
            }
        }
        throw new IllegalArgumentException("No encryption key found");
    }

    public PGPPrivateKey extractPrivateKey(PGPSecretKey secretKey, char[] passPhrase) throws Exception {
        return secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider(new BouncyCastleProvider()).build(passPhrase));
    }

    public void encryptAndSign(OutputStream out, InputStream data, PGPPublicKey encKey, PGPPrivateKey signKey) throws Exception {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
        OutputStream comOut = comData.open(bOut);
        
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(
            new JcaPGPContentSignerBuilder(signKey.getPublicKeyPacket().getAlgorithm(), PGPUtil.SHA512)
                .setProvider(new BouncyCastleProvider()));
        sGen.init(PGPSignature.BINARY_DOCUMENT, signKey);
        
        Iterator it = encKey.getUserIDs();
        if (it.hasNext()) {
            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, (String) it.next());
            sGen.setHashedSubpackets(spGen.generate());
        }
        
        sGen.generateOnePassVersion(false).encode(comOut);
        
        PGPLiteralDataGenerator lGen = new PGPLiteralDataGenerator();
        OutputStream lOut = lGen.open(comOut, PGPLiteralData.BINARY, "Sample-Data", new Date(), new byte[data.available()]);
        
        byte[] buf = new byte[4096];
        int len;
        while ((len = data.read(buf)) > 0) {
            lOut.write(buf, 0, len);
            sGen.update(buf, 0, len);
        }
        
        lOut.close();
        lGen.close();
        sGen.generate().encode(comOut);
        comOut.close();
        comData.close();
        
        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
            new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256)
                .setWithIntegrityPacket(true)
                .setSecureRandom(new SecureRandom())
                .setProvider(new BouncyCastleProvider()));
        encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider(new BouncyCastleProvider()));
        
        byte[] bytes = bOut.toByteArray();
        OutputStream cOut = encGen.open(out, bytes.length);
        cOut.write(bytes);
        cOut.close();
    }

    public void decryptStream(InputStream in, OutputStream out, PGPPrivateKey privateKey, PGPPublicKey publicKey) throws Exception {
        PGPObjectFactory pgpF = new PGPObjectFactory(PGPUtil.getDecoderStream(in), new JcaKeyFingerprintCalculator());
        Object o = pgpF.nextObject();
        
        PGPEncryptedDataList enc = (o instanceof PGPEncryptedDataList) ? (PGPEncryptedDataList) o : (PGPEncryptedDataList) pgpF.nextObject();
        
        PGPPublicKeyEncryptedData pbe = (PGPPublicKeyEncryptedData) enc.get(0);
        InputStream clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder()
            .setProvider(new BouncyCastleProvider())
            .setContentProvider(new BouncyCastleProvider())
            .build(privateKey));
        
        PGPObjectFactory plainFact = new PGPObjectFactory(clear, new JcaKeyFingerprintCalculator());
        Object message = plainFact.nextObject();
        
        PGPOnePassSignatureList onePassSignatureList = null;
        PGPSignatureList signatureList = null;
        ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
        
        while (message != null) {
            if (message instanceof PGPCompressedData) {
                PGPCompressedData cData = (PGPCompressedData) message;
                plainFact = new PGPObjectFactory(cData.getDataStream(), new JcaKeyFingerprintCalculator());
                message = plainFact.nextObject();
            }
            
            if (message instanceof PGPLiteralData) {
                Streams.pipeAll(((PGPLiteralData) message).getInputStream(), actualOutput);
            } else if (message instanceof PGPOnePassSignatureList) {
                onePassSignatureList = (PGPOnePassSignatureList) message;
            } else if (message instanceof PGPSignatureList) {
                signatureList = (PGPSignatureList) message;
            }
            message = plainFact.nextObject();
        }
        
        byte[] output = actualOutput.toByteArray();
        
        if (onePassSignatureList != null && signatureList != null && publicKey != null) {
            PGPOnePassSignature ops = onePassSignatureList.get(0);
            ops.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), publicKey);
            ops.update(output);
            
            if (!ops.verify(signatureList.get(0))) {
                throw new Exception("Signature verification failed");
            }
        }
        
        out.write(output);
        out.flush();
    }
    
    public byte[] decryptAndVerify(byte[] encrypted, PGPPrivateKey privateKey, PGPPublicKey publicKey) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        decryptStream(new java.io.ByteArrayInputStream(encrypted), out, privateKey, publicKey);
        return out.toByteArray();
    }
    
    public PGPSecretKey generateTestSecretKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(1024); // Smaller key size for testing
        KeyPair kp = kpg.generateKeyPair();
        
        PGPKeyPair keyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, kp, new Date());
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(
            PGPSignature.POSITIVE_CERTIFICATION, keyPair, "test@test.com",
            new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1),
            null, null, 
            new JcaPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1).setProvider("BC"),
            new JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.CAST5, new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1))
                .setProvider("BC").build("test".toCharArray()));
        
        return keyRingGen.generateSecretKeyRing().getSecretKey();
    }
    
    public PGPPublicKey generateTestPublicKey() throws Exception {
        return generateTestSecretKey().getPublicKey();
    }
}
