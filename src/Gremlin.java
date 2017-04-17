import java.net.*;
import java.util.Arrays;

/**
 * Corrupts packet data, delays packet, or drops packet based on a passed parameter chance,
 * avoiding corruption of the header.
 *
 * @author GROUP 10 - Joseph Schultz (jjs0021), Cameron Schaerfl (crs0051), Erich Wu (ejw0013)
 * @version 3-30-2017
 */

public class Gremlin {

    private static final int PACKET_SIZE = 512;
    private static final int CHECKSUM_SIZE = 2;
    private static final int SEQUENCE_SIZE = 2;
    private static final int DATA_OFFSET = 4;
    private static final int CHECKSUM_OFFSET = 0;
    private static final int SEQUENCE_OFFSET = 2;
    private double dmgChance;
    private double dropChance;
    private double delayChance;
    private int delayTime;

    /**
     * Constructs a gremlin based on the passed in chances and time.
     * Chance is restricted to a double between 0 and 1 inclusive.
     *
     * @param dmgIn       the chance that a packet is damaged.
     * @param dropIn      the chance a packet is dropped
     * @param delayIn     the chance a packet is delayed
     * @param delayTimeIn the time a packet is delayed in milliseconds
     */
    public Gremlin(double dmgIn, double dropIn, double delayIn, int delayTimeIn) {
        //damage chance
        if (dmgIn < 0) {
            dmgChance = 0;
        } else if (dmgIn > 1) {
            dmgChance = 1;
        } else {
            dmgChance = dmgIn;
        }//drop chance
        if (dropIn < 0) {
            dropChance = 0;
        } else if (dropIn > 1) {
            dropChance = 1;
        } else {
            dropChance = dropIn;
        }//delay chance
        if (delayIn < 0) {
            delayChance = 0;
        } else if (delayIn > 1) {
            delayChance = 1;
        } else {
            delayChance = delayIn;
        }//delay Time
        delayTime = delayTimeIn;
    }

    /**
     * Corrupts byte(s), delays packet relay, or drops packet
     * Dropped packet will cause returned packet to be null
     *
     * @param packetIn the packet to touch
     * @return DatagramPacket the packet with the corrupted or non corrupted data
     */
    public DatagramPacket touchPacket(DatagramPacket packetIn)
            throws InterruptedException {
        DatagramPacket packetOut = packetIn;
        byte[] packetData = packetIn.getData();
        int messageOffset = DATA_OFFSET;
        int messageLength = packetIn.getLength() - messageOffset;
        byte[] terminator = {'\u0000'};

        if (!(Arrays.equals(packetData, terminator))) {
            //chance to drop packet
            double roll = Math.random();
            if ((dropChance != 0) && (roll <= dropChance)) {
                packetOut = null;
            } else {
                //chance to corrupt packet
                roll = Math.random();
                if ((dmgChance != 0) && (roll <= dmgChance)) {
                    int numChanges = 1;
                    roll = Math.random();
                    if (roll <= .2) {
                        numChanges = 3;
                    } else if (roll <= .5) {
                        numChanges = 2;
                    }
                    int[] byteToChange = new int[numChanges];
                    for (int i = 0; i < (numChanges); i++) {
                        byteToChange[i] = ((int) (Math.random() * messageLength) + messageOffset);
                        if (i > 0) { //check that duplicate corruption not made
                            for (int j = 0; j < i; j++) {
                                if (byteToChange[j] == byteToChange[i]) {
                                    i--;
                                    break;
                                }
                            }
                        }
                    }
                    for (int i = 0; i < numChanges; i++) {
                        packetData[byteToChange[i]] = (byte) (packetData[byteToChange[i]] + 1);
                    }
                    packetOut.setData(packetData);
                }
                //chance to delay packet
                roll = Math.random();
                if ((delayChance != 0) && (roll <= delayChance)) {
                    Thread.sleep(delayTime);
                }
            }
        }
        return packetOut;
    }
}