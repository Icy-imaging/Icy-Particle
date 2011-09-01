/**
 * 
 */
package plugins.stef.demo.particle;

/**
 * @author user
 */
public class Particle
{
    float positionX;
    float positionY;
    float moveX;
    float moveY;
    float gravity;
    float rebondX;
    float rebondY;
    float size;

    byte intensity;

    boolean active;

    public Particle()
    {
        positionX = 0f;
        positionY = 0f;
        moveX = 0f;
        moveY = 0f;
        gravity = 0f;
        rebondX = 0f;
        rebondY = 0f;
        size = 1f;

        intensity = 0;

        active = false;
    }
}
