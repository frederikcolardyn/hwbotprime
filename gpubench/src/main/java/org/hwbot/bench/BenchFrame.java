package org.hwbot.bench;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;

import org.hwbot.bench.service.HardwareService;

import com.jogamp.opengl.util.Animator;

public class BenchFrame extends Frame implements GLEventListener, WindowListener {

	private static final long serialVersionUID = 1L;

	public BenchFrame() {
		super("HWBOT GPU Bench");
		
		System.out.println("Benchframe");

		setLayout(new BorderLayout());
		addWindowListener(this);

		setSize(600, 600);
		setLocation(40, 40);

		setVisible(true);

		try {
			HardwareService.extractFile("libjogl_awt.jnilib", new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator
					+ "libjogl_awt.jnilib"), true);
			HardwareService.extractFile("libjogl_cg.jnilib", new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator
					+ "libjogl_cg.jnilib"), true);
			HardwareService.extractFile("libjogl.jnilib", new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator + "libjogl.jnilib"),
					true);

			String path = System.getProperty("java.library.path");
			System.setProperty("java.library.path", path + ":" + System.getProperty("java.io.tmpdir"));
			System.out.println("java.library.path: " + System.getProperty("java.library.path"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		setupJOGL();
	}

	public static void main(String[] args) {
		BenchFrame demo = new BenchFrame();
		demo.setVisible(true);
	}

	private void setupJOGL() {
		try {
			System.out.println("Setting up...");
			// GLCapabilities caps = new GLCapabilities(GLProfile.getGL2GL3());
			// caps.setDoubleBuffered(true);
			// caps.setHardwareAccelerated(true);
			GLCanvas canvas = new javax.media.opengl.awt.GLCanvas();
			System.out.println("Created canvas.");
			canvas.addGLEventListener(this);

			add(canvas, BorderLayout.CENTER);

			System.out.println("Starting animation...");
			Animator anim = new Animator(canvas);
			anim.setRunAsFastAsPossible(true);
			anim.start();
			System.out.println("Done!");
		} catch (Throwable e) {
			System.out.println("Error! " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void init(GLAutoDrawable drawable) {
		GL gl = drawable.getGL();

		gl.glClearColor(0, 0, 0, 0);
		// gl.glMatrixMode(GL.GL_PROJECTION);
		// gl.glLoadIdentity();
		// gl.glOrtho(0, 1, 0, 1, -1, 1);
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		System.out.println("reshape");
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
		System.out.println("displayChanged");
	}

	public void display(GLAutoDrawable drawable) {
		System.out.println("displaying");
		GL gl = drawable.getGL();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		gl.glFlush();

	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowClosed(WindowEvent arg0) {
		System.exit(0);
	}

	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub

	}

}
