import ij.gui.GenericDialog;

import java.awt.TextField;

import java.io.FileOutputStream;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbSession;

import jcifs.UniAddress;

public class Get {

	public static void main( String argv[] ) throws Exception {
		NtlmPasswordAuthentication auth = null;

		SmbFile f;
		SmbFileInputStream in;

		for (;;) try {
			f = new SmbFile(argv[0], auth);
			if (f.isDirectory()) {
				try {
					listDirectory(f);
				} catch (SmbAuthException e) {
					auth = getCredentials();
					if (auth == null)
						System.exit(1);
					f = new SmbFile(argv[0], auth);
					listDirectory(f);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
			in = new SmbFileInputStream(f);
			break;
		} catch (SmbAuthException e) {
			auth = getCredentials();
			if (auth == null)
				System.exit(1);
		}

		FileOutputStream out = new FileOutputStream(f.getName());

		long t0 = System.currentTimeMillis();

		byte[] b = new byte[8192];
		int n, tot = 0;
		long t1 = t0;
		while(( n = in.read( b )) > 0 ) {
			out.write( b, 0, n );
			tot += n;
			System.out.print( '#' );
		}

		long t = System.currentTimeMillis() - t0;

		System.out.println();
		System.out.println(tot + " bytes transfered in " + ( t / 1000 )
				+ " seconds at " + (( tot / 1000 ) /
					Math.max( 1, ( t / 1000 )))
				+ "Kbytes/sec");

		in.close();
		out.close();

		System.exit(0);
	}

	static NtlmPasswordAuthentication getCredentials() {
		GenericDialog gd = new GenericDialog("Logon required");
		gd.addStringField("Domain", "", 20);
		gd.addStringField("User_name", "", 20);
		gd.addStringField("Password", "", 20);
		((TextField)gd.getStringFields().lastElement())
			.setEchoChar('*');
		gd.showDialog();
		if (gd.wasCanceled())
			return null;

		String domain = gd.getNextString();
		String user = gd.getNextString();
		String password = gd.getNextString();
		return new NtlmPasswordAuthentication(domain, user, password);
	}

	static void listDirectory(SmbFile file) throws SmbException {
		long t1 = System.currentTimeMillis();
		String[] files = file.list();
		long t2 = System.currentTimeMillis() - t1;

		for (int i = 0; i < files.length; i++)
			System.out.println(files[i]);

		System.out.println();
		System.out.println(files.length + " files in " + t2 + "ms");
	}
}
