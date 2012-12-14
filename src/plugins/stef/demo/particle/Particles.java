/**
 * 
 */
package plugins.stef.demo.particle;

import icy.canvas.Canvas3D;
import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.math.FPSMeter;
import icy.painter.AbstractPainter;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.type.DataType;
import icy.util.EventUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.Random;

/**
 * Particles physic demonstration plugin.
 * 
 * @author Stephane
 */
public class Particles extends PluginActionable
{
    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGTH = 480;

    private static final int NUM_PARTICLE = 100000;
    private static final int MARGE_BORDER = 10;

    /**
     * Painter class, used to retrieve mouse events on image.
     * 
     * @author Stephane
     */
    private class ParticlePainter extends AbstractPainter
    {
        public ParticlePainter()
        {
            super();
        }

        @Override
        public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas)
        {
            if (EventUtil.isAltDown(e))
                return;

            final int button = e.getButton();

            switch (button)
            {
                case MouseEvent.BUTTON1:
                    mouseBL = false;
                    e.consume();
                    break;
            }
        }

        @Override
        public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas)
        {
            if (EventUtil.isAltDown(e))
                return;

            final int button = e.getButton();

            switch (button)
            {
                case MouseEvent.BUTTON1:
                    mouseBL = true;
                    // displayToolTip = false;
                    e.consume();
                    break;
            }
        }

        @Override
        public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas)
        {
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_SPACE:
                    fastDraw = !fastDraw;
                    e.consume();
                    break;
            }
        }

        @Override
        public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas)
        {
            // move source with mouse
            updateSource(imagePoint);
        }

        @Override
        public void mouseDrag(MouseEvent e, Point2D imagePoint, IcyCanvas canvas)
        {
            // move source with mouse
            updateSource(imagePoint);
        }

        @Override
        public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
        {
            if (canvas instanceof Canvas3D)
            {
                System.out.println("Particles do no support Canvas3D :");
                return;
            }

            if (g != null)
            {
                final Graphics2D g2 = (Graphics2D) g.create();

                if (fastDraw)
                    g2.drawImage(bufImage, 0, 0, null);

                g2.setColor(Color.darkGray);
                g2.drawString(fpsMessage, 11, 21);
                g2.setColor(Color.white);
                g2.drawString(fpsMessage, 10, 20);

                if (fastDraw)
                {
                    g2.setColor(Color.darkGray);
                    g2.drawString("Use SPACE key to switch to normal image draw (LUT support)", 11, 41);
                    g2.setColor(Color.white);
                    g2.drawString("Use SPACE key to switch to normal image draw (LUT support)", 10, 40);
                }
                else
                {
                    g2.setColor(Color.darkGray);
                    g2.drawString("Use SPACE key to switch to fast painter draw mode (no LUT support)", 11, 41);
                    g2.setColor(Color.white);
                    g2.drawString("Use SPACE key to switch to fast painter draw mode (no LUT support)", 10, 40);
                }

                g2.setColor(Color.darkGray);
                g2.drawString("Maintain ALT key for normal canvas operation on mouse drag", 11, 61);
                g2.setColor(Color.white);
                g2.drawString("Maintain ALT key for normal canvas operation on mouse drag", 10, 60);

                if (displayToolTip)
                {
                    g2.setColor(Color.darkGray);
                    g2.drawString("Maintain left mouse button to add new particles", 11, 81);
                    g2.setColor(Color.white);
                    g2.drawString("Maintain left mouse button to add new particles", 10, 80);
                }

                g2.dispose();
            }
        }
    }

    // particles table
    private final Particle[] particles;
    // displayed image
    final IcyBufferedImage icyImage;
    // displayed image
    final BufferedImage bufImage;
    // cache buffer
    private final byte[] buffer;

    private final Random random;
    private final FPSMeter fpsMeter;
    String fpsMessage;
    private final ParticlePainter painter;
    Sequence sequence;

    float sourceX;
    float sourceY;

    boolean mouseBL;
    boolean displayToolTip;

    boolean terminated;
    boolean fastDraw;

    public Particles()
    {
        super();

        // create our image & our back buffer
        icyImage = new IcyBufferedImage(IMAGE_WIDTH, IMAGE_HEIGTH, 1, DataType.UBYTE);
        bufImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGTH, BufferedImage.TYPE_BYTE_GRAY);
        buffer = new byte[IMAGE_HEIGTH * IMAGE_WIDTH];

        random = new Random();
        fpsMeter = new FPSMeter();
        fpsMessage = "";

        // source init
        sourceX = IMAGE_WIDTH / 2f;
        sourceY = IMAGE_HEIGTH / 2f;

        mouseBL = false;
        displayToolTip = true;

        // particles init
        particles = new Particle[NUM_PARTICLE];
        for (int i = 0; i < particles.length; i++)
        {
            final Particle partic = new Particle();

            particles[i] = partic;
            initPartic(partic);
        }

        // create our painter
        painter = new ParticlePainter();

        // create sequence
        sequence = new Sequence("Particle animation", icyImage);
        // add our painter
        sequence.addPainter(painter);
        // add listener
        sequence.addListener(new SequenceListener()
        {
            @Override
            public void sequenceClosed(Sequence seq)
            {
                // end process when sequence is closed
                terminated = true;
                seq.removeListener(this);
                // remove reference
                sequence = null;
            }

            @Override
            public void sequenceChanged(SequenceEvent sequenceEvent)
            {
                // nothing to do
            }
        });

        // add sequence to application
        addSequence(sequence);

        // process can start
        terminated = false;
        fastDraw = true;
    }

    private void initPartic(Particle partic)
    {
        partic.positionX = sourceX + random.nextFloat();
        partic.positionY = sourceY + random.nextFloat();
        partic.moveX = 0f;
        partic.moveY = 0f;
        partic.gravity = 40f + (random.nextFloat() * 50f);
        // partic.rebondX = 0d;
        // partic.rebondY = 0d;
        // partic.size = 1d;
        partic.intensity = (byte) random.nextInt(0x40);
        // partic is now active
        partic.active = true;
    }

    void animPartic(Particle partic)
    {
        // movement
        partic.positionX += partic.moveX;
        partic.positionY += partic.moveY;

        partic.active = checkPartic(partic);

        if (partic.active)
        {
            final float fDistX = sourceX - partic.positionX;
            final float fDistY = sourceY - partic.positionY;
            final float fDist = (float) Math.sqrt((fDistX * fDistX) + (fDistY * fDistY));

            if (fDist != 0f)
            {
                // attraction force is calculated from particle "weight" and distance
                final float fForce = partic.gravity * fDist;

                // impact particle movement by attraction force
                partic.moveX += fDistX / fForce;
                partic.moveY += fDistY / fForce;
            }
        }
    }

    boolean checkPartic(Particle partic)
    {
        // check bounds
        return (partic.positionX >= 0f) && (partic.positionY >= 0f) && (partic.positionX < IMAGE_WIDTH)
                && (partic.positionY < IMAGE_HEIGTH);
    }

    void drawPartic(Particle partic)
    {
        final int x = (int) partic.positionX;
        final int y = (int) partic.positionY;

        final int offset = (y * IMAGE_WIDTH) + x;
        final int value = (buffer[offset] & 0xFF) + (partic.intensity & 0xFF);

        buffer[offset] = (byte) ((value > 0xFF) ? 0xFF : value);
    }

    private void addActivePartic(int num)
    {
        // add active particle
        if (num <= 0)
            return;

        int notDone = num;
        for (Particle partic : particles)
        {
            if (!partic.active)
            {
                initPartic(partic);
                if (--notDone == 0)
                    return;
            }
        }
    }

    public void process()
    {
        while (!terminated)
        {
            // scan command
            scanCommand();
            // process
            updateFrame();
        }
    }

    void updateSource(Point2D location)
    {
        float x = (float) location.getX();
        float y = (float) location.getY();

        // check source is not out of bounds
        if (x < MARGE_BORDER)
            x = MARGE_BORDER + 1;
        else if (x > (IMAGE_WIDTH - MARGE_BORDER))
            x = (IMAGE_WIDTH - MARGE_BORDER) - 1;
        if (y < MARGE_BORDER)
            y = MARGE_BORDER + 1;
        else if (y > (IMAGE_HEIGTH - MARGE_BORDER))
            y = (IMAGE_HEIGTH - MARGE_BORDER) - 1;

        sourceX = x;
        sourceY = y;
    }

    private void updateFPSCounter(int numPartic)
    {
        fpsMessage = fpsMeter.update() + " simulated frames per second -   " + numPartic + " particules";
    }

    void scanCommand()
    {
        // add partics
        if (mouseBL)
            addActivePartic(200);
    }

    void updateFrame()
    {
        // animate
        for (Particle partic : particles)
            if (partic.active)
                animPartic(partic);

        // clear
        Arrays.fill(buffer, (byte) 0);

        // draw partics
        int activePart = 0;
        for (Particle partic : particles)
        {
            if (partic.active)
            {
                drawPartic(partic);
                activePart++;
            }
        }

        displayToolTip = (activePart != NUM_PARTICLE);

        // update FPS
        updateFPSCounter(activePart);

        if (fastDraw)
        {
            // copy data to the buffered image
            System.arraycopy(buffer, 0, ((DataBufferByte) bufImage.getRaster().getDataBuffer()).getData(), 0,
                    buffer.length);
            // notify painter has changed
            painter.changed();
        }
        else
            // copy data to image (this automatically cause a repaint)
            icyImage.setDataXYAsByte(0, buffer);
    }

    @Override
    public void run()
    {
        // start processing in a external thread (don't lock AWT)
        final Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                process();
            }
        });

        // set priority to minimum so image refresh is done in time
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
}
