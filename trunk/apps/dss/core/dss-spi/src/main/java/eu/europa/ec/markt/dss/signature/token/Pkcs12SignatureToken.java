/*
 * DSS - Digital Signature Services
 *
 * Copyright (C) 2013 European Commission, Directorate-General Internal Market and Services (DG MARKT), B-1049 Bruxelles/Brussel
 *
 * Developed by: 2013 ARHS Developments S.A. (rue Nicolas Bové 2B, L-1253 Luxembourg) http://www.arhs-developments.com
 *
 * This file is part of the "DSS - Digital Signature Services" project.
 *
 * "DSS - Digital Signature Services" is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * DSS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * "DSS - Digital Signature Services".  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.europa.ec.markt.dss.signature.token;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.BadPaddingException;

import eu.europa.ec.markt.dss.DSSUtils;
import eu.europa.ec.markt.dss.exception.DSSBadPasswordException;
import eu.europa.ec.markt.dss.exception.DSSBadPasswordException.MSG;
import eu.europa.ec.markt.dss.exception.DSSException;

/**
 * Class holding all PKCS#12 file access logic.
 *
 * @version $Revision: 980 $ - $Date: 2011-06-16 14:17:13 +0200 (jeu., 16 juin 2011) $
 */

public class Pkcs12SignatureToken extends AbstractSignatureTokenConnection {

    private char[] password;

    private File pkcs12File;
    private byte[] pkcs12Data;

    /**
     * Creates a SignatureTokenConnection with the provided password and path to PKCS#12 file.
     *
     * @param password
     * @param pkcs12FilePath
     */
    public Pkcs12SignatureToken(String password, String pkcs12FilePath) {
        this(password.toCharArray(), new File(pkcs12FilePath));
    }

    /**
     * Creates a SignatureTokenConnection with the provided password and path to PKCS#12 file.
     *
     * @param password
     * @param pkcs12FilePath
     */
    public Pkcs12SignatureToken(char[] password, String pkcs12FilePath) {
        this(password, new File(pkcs12FilePath));
    }

    /**
     * Creates a SignatureTokenConnection with the provided password and path to PKCS#12 file object.
     *
     * @param password
     * @param pkcs12File
     */
    public Pkcs12SignatureToken(String password, File pkcs12File) {
        this(password.toCharArray(), pkcs12File);
    }

    /**
     * Creates a SignatureTokenConnection with the provided password and PKCS#12 file object.
     *
     * @param password
     * @param pkcs12File
     */
    public Pkcs12SignatureToken(char[] password, File pkcs12File) {
        this.password = password;
        if (!pkcs12File.exists()) {
            throw new DSSException("File Not Found " + pkcs12File.getAbsolutePath());
        }
        this.pkcs12File = pkcs12File;
    }

    /**
     * A specific constructor to allow non-file based usage of p12 data
     *
     * @param password
     * @param pkcs12Data
     */
    public Pkcs12SignatureToken(char[] password, byte[] pkcs12Data) {

        this.password = password;
        if (pkcs12Data == null) {
            throw new DSSException("PKCS12 data not provided");
        }
        this.pkcs12Data = pkcs12Data;
    }

    /**
     * A specific constructor to allow non-file based usage of p12 data
     *
     * @param password
     * @param inputStream
     */
    public Pkcs12SignatureToken(String password, InputStream inputStream) {

        this.password = password.toCharArray();
        if (inputStream == null) {
            throw new RuntimeException("PKCS12 data not provided");
        }
        this.pkcs12Data = DSSUtils.toByteArray(inputStream);
    }

    @Override
    public void close() {
    }

    @Override
    public List<DSSPrivateKeyEntry> getKeys() throws DSSException {

        List<DSSPrivateKeyEntry> list = new ArrayList<DSSPrivateKeyEntry>();

        InputStream input = null;
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            if (pkcs12Data != null) {
                input = new ByteArrayInputStream(pkcs12Data);
            } else {
                input = new FileInputStream(pkcs12File);
            }

            keyStore.load(input, password);
            PasswordProtection pp = new KeyStore.PasswordProtection(password);
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {

                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {

                    PrivateKeyEntry entry = (PrivateKeyEntry) keyStore.getEntry(alias, pp);
                    final KSPrivateKeyEntry privateKeyEntry = new KSPrivateKeyEntry(entry);
                    list.add(privateKeyEntry);
                }
            }
        } catch (Exception e) {
            if (e.getCause() instanceof BadPaddingException) {
                throw new DSSBadPasswordException(MSG.PKCS12_BAD_PASSWORD);
            }
            throw new DSSException("Can't initialize Sun PKCS#12 security provider. Reason: " + getCauseMessage(e), e);
        } finally {
            DSSUtils.closeQuietly(input);
        }
        return list;
    }
}