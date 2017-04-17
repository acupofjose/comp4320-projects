import java.net.*;
import java.util.Arrays;

/**
 * Corrupts packet data based on a passed parameter chance, avoiding corruption of the header.
 * @author GROUP 10 - Joseph Schultz (jjs0021), Cameron Schaerfl (crs0051), Erich Wu (ejw0013)
 * @version 3-21-2017
 */

public class Gremlin {

   private static final int PACKET_SIZE = 128;
   private static final int CHECKSUM_SIZE = 2;
   private static final int SEQUENCE_SIZE = 2;
   private static final int DATA_OFFSET = 4;
   private static final int CHECKSUM_OFFSET = 0;
   private static final int SEQUENCE_OFFSET = 2;
   private double dmgChance;

   /**
    * Constructs a gremlin based on the passed in damage chance
    * Chance is restricted to a double between 0 and 1 inclusive
    * @param dmgIn the chance that a packet is damaged.
     */
   public Gremlin(double dmgIn) {
      if (dmgIn < 0) {
         dmgChance = 0;
      } 
      else if (dmgIn > 1) {
         dmgChance = 1;
      } 
      else {
         dmgChance = dmgIn;
      }
   }

    /**
     * Corrupts byte(s) within the data of a datagram packet
     * @param packetIn the packet to touch
     * @return DatagramPacket the packet with the corrupted or non corrupted data
     */
   public DatagramPacket touchPacket(DatagramPacket packetIn) {
      DatagramPacket packetOut = packetIn;
      byte[] packetData = packetIn.getData();
      int messageOffset = DATA_OFFSET;
      int messageLength = packetIn.getLength() - messageOffset;
      byte[] terminator = {'\u0000'};
      
      if (!(Arrays.equals(packetData, terminator))) {
         double roll = Math.random();
         if ((dmgChance != 0) && (roll <= dmgChance)) {
            int numChanges = 1;
            roll = Math.random();
            if (roll <= .2) {
               numChanges = 3;
            } 
            else if (roll <= .5) {
               numChanges = 2;
            }
            int[] byteToChange = new int[numChanges];
            for (int i = 0; i < (numChanges); i++) {
               byteToChange[i] = ((int)(Math.random() * messageLength) + messageOffset);
               if (i > 0) {
                  for (int j = 0; j < i; j++) {
                     if(byteToChange[j] == byteToChange[i]) {
                        i--;
                        break;
                     }
                  }
               }
            }
            for (int i = 0; i < numChanges; i++) {
               packetData[byteToChange[i]] = (byte)(packetData[byteToChange[i]] + 1);
            }
            packetOut.setData(packetData);
         }
      }
      return packetOut;
   }
}