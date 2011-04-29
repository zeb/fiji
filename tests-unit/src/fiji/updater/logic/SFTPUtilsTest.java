/*
 * Copyright (c) 2011 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek at ieee.org
 *
 */
package fiji.updater.logic;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Jarek Sacha
 * @since 4/21/11 2:16 PM
 */
public final class SFTPUtilsTest {

    private SFTPOperations sftp;


    @BeforeClass
    public static void setUp() {
        // Setup logging
        final Handler[] handlers = Logger.getLogger("").getHandlers();
        final ConsoleHandler ch;
        if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
            ch = (ConsoleHandler) handlers[0];
        } else {
            ch = new ConsoleHandler();
            Logger.getLogger("").addHandler(ch);
        }
        ch.setLevel(Level.FINEST);
        Logger.getLogger("").setLevel(Level.FINEST);
    }


    @Before
    public void openConnection() throws JSchException {

        final String user = "jsacha,ij-plugins";
        final String host = "frs.sourceforge.net";
        final String password = "frs.sourceforge.net";

        sftp = new SFTPOperations(user, host, new MyUserInfo(password));
    }


    @After
    public void closeConnection() throws IOException {
        if (sftp != null) {
            sftp.disconnect();
            sftp = null;
        }
    }


    @Test
    public void testUploadLockRename() throws Exception {

        final InputStream in = new ByteArrayInputStream("".getBytes());
        final String uploadDest = "/home/frs/project/i/ij/ij-plugins/fiji_update/";
        final String fileName = "sample-db.xml.gz";
        final String dest = uploadDest + fileName;
        final String destLock = uploadDest + fileName + ".lock";


        // Upload locked
        sftp.put(in, destLock);
        assertTrue(sftp.fileExists(destLock));

        sftp.rename(destLock, dest);

        // Execute stat to test existence of uploaded file
        assertFalse(sftp.fileExists(destLock));
        assertTrue(sftp.fileExists(dest));

//        sftp.rm(dest);
//        assertFalse(SFTPUtils.fileExists(sftp, dest));
    }


    @Test
    public void testTimestamp() throws Exception {

        final String uploadDest = "/home/frs/project/i/ij/ij-plugins/fiji_update/";

        final InputStream in = new ByteArrayInputStream("".getBytes());
        final String destFile = uploadDest + "timestamp";

        sftp.put(in, destFile);
        final long mTime1 = sftp.timestamp(destFile);
        sftp.rm(destFile);
        assertTrue(mTime1 != 0);

        Thread.sleep(1500);

        sftp.put(in, destFile);
        final long mTime2 = sftp.timestamp(destFile);
        sftp.rm(destFile);
        assertTrue(mTime2 != 0);

        assertTrue(mTime2 > mTime1);
    }


    @Test
    public void createDirectory() throws SftpException {
        final String uploadDest = "/home/frs/project/i/ij/ij-plugins/fiji_update/";
        final InputStream in = new ByteArrayInputStream("".getBytes());
        final String destFile = uploadDest + "one/two/three/timestamp";
        sftp.mkParentDirs(uploadDest + "one/two/three/timestamp");

//        sftp.mkdir(uploadDest + "one");
//        sftp.mkdir(uploadDest + "one/two/three");
        sftp.put(in, destFile);
    }


    final private static class MyUserInfo implements UserInfo {

        private final String password;


        public MyUserInfo(final String password) {
            this.password = password;
        }


        @Override
        public String getPassphrase() {
            return null;
        }


        @Override
        public String getPassword() {
            return null;
        }


        @Override
        public boolean promptPassword(String s) {
            return true;
        }


        @Override
        public boolean promptPassphrase(String s) {
            return true;
        }


        @Override
        public boolean promptYesNo(String s) {
            return true;
        }


        @Override
        public void showMessage(String s) {
        }
    }
}