package com.tomazbr9.licensing.tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;

import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.HexFormat;
import java.util.Set;

public final class GenerateLicenseKeys {

    private static final String PRIVATE_NAME =
            "auto-nota-private.pk8";

    private static final String PUBLIC_NAME =
            "auto-nota-public.der";

    private GenerateLicenseKeys() {
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Informe o diretorio de destino das chaves."
            );
        }

        Path destino = Path.of(args[0])
                .toAbsolutePath()
                .normalize();

        Path projeto = Path.of("")
                .toAbsolutePath()
                .normalize();

        // Impede gravar as chaves dentro do projeto
        if (destino.startsWith(projeto)) {
            throw new IllegalArgumentException(
                    "Por seguranca, use um diretorio fora do projeto."
            );
        }

        Path privada = destino.resolve(PRIVATE_NAME);
        Path publica = destino.resolve(PUBLIC_NAME);

        // Nunca sobrescrever chaves existentes
        if (Files.exists(privada) || Files.exists(publica)) {
            throw new IllegalStateException(
                    "Chaves ja existem. Geracao cancelada."
            );
        }

        Files.createDirectories(destino);

        // Restringe o acesso ao diretorio no Linux
        if (Files.getFileStore(destino)
                .supportsFileAttributeView("posix")) {

            Files.setPosixFilePermissions(
                    destino,
                    PosixFilePermissions.fromString("rwx------")
            );
        }

        // 1. Gera o par de chaves Ed25519
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("Ed25519");

        KeyPair par = generator.generateKeyPair();

        // 2. Salva a chave privada no formato PKCS#8
        gravarNovo(
                privada,
                par.getPrivate().getEncoded(),
                "rw-------"
        );

        // 3. Salva a chave publica no formato X.509
        gravarNovo(
                publica,
                par.getPublic().getEncoded(),
                "rw-r--r--"
        );

        // 4. Recarrega as chaves salvas
        KeyFactory factory =
                KeyFactory.getInstance("Ed25519");

        var chavePrivada = factory.generatePrivate(
                new PKCS8EncodedKeySpec(
                        Files.readAllBytes(privada)
                )
        );

        var chavePublica = factory.generatePublic(
                new X509EncodedKeySpec(
                        Files.readAllBytes(publica)
                )
        );

        // 5. Assina uma mensagem de teste
        byte[] mensagem =
                "teste-auto-nota-ed25519".getBytes(
                        StandardCharsets.UTF_8
                );

        Signature assinador =
                Signature.getInstance("Ed25519");

        assinador.initSign(chavePrivada);
        assinador.update(mensagem);

        byte[] assinatura = assinador.sign();

        // 6. Verifica a assinatura
        Signature verificador =
                Signature.getInstance("Ed25519");

        verificador.initVerify(chavePublica);
        verificador.update(mensagem);

        if (!verificador.verify(assinatura)) {
            throw new IllegalStateException(
                    "Falha na verificacao das chaves."
            );
        }

        // 7. Calcula uma identificacao da chave publica
        byte[] fingerprint =
                MessageDigest.getInstance("SHA-256")
                        .digest(
                                Files.readAllBytes(publica)
                        );

        System.out.println(
                "Par Ed25519 gerado e validado!"
        );

        System.out.println(
                "Chave privada: " + privada
        );

        System.out.println(
                "Chave publica: " + publica
        );

        System.out.println(
                "SHA-256 da chave publica: "
                        + HexFormat.of().formatHex(fingerprint)
        );
    }

    private static void gravarNovo(
            Path arquivo,
            byte[] conteudo,
            String permissoes
    ) throws IOException {

        if (Files.getFileStore(arquivo.getParent())
                .supportsFileAttributeView("posix")) {

            Set<OpenOption> opcoes = Set.of(
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            try (SeekableByteChannel canal =
                         Files.newByteChannel(
                                 arquivo,
                                 opcoes,
                                 PosixFilePermissions.asFileAttribute(
                                         PosixFilePermissions.fromString(
                                                 permissoes
                                         )
                                 )
                         )) {

                ByteBuffer buffer =
                        ByteBuffer.wrap(conteudo);

                while (buffer.hasRemaining()) {
                    canal.write(buffer);
                }
            }

        } else {
            Files.write(
                    arquivo,
                    conteudo,
                    StandardOpenOption.CREATE_NEW
            );
        }
    }
}
