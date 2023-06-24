package com.codebase;

import java.util.*;
import java.io.*;

public class Dissassembler {
    private class OpcodeMapValue {
        public String command;
        public boolean requiresModRMByte;
        public String commandType;
        public int digit;
        public OpcodeMapValue(String command, boolean requiresModRMByte, String commandType) {
            this.command = command;
            this.requiresModRMByte = requiresModRMByte;
            this.commandType = commandType;
        }
        public void setDigit(int digit) {
          this.digit = digit;
        }
    }

    private class InstructionValues {
      public String instructionBytes = "";
      public String instruction = "";
      public InstructionValues(String instructionBytes, String instruction) {
        this.instructionBytes = instructionBytes;
        this.instruction = instruction;
      }
    }
    public HashMap<String, String> outputList = new HashMap<String, String>();
    private final HashMap<Integer, OpcodeMapValue[]> opcode_map_values;
    private final String[] global_register_names = new String[]{"eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi" };
    private ArrayList<Long> buffer;
    private HashMap<String, String> labelMap = new HashMap<String, String>();
    private long index;
    private long instructionStartIndex;
    private InstructionValues instructionValues;

    public Dissassembler() {
        this.opcode_map_values = this.get_opcode_map_values();
    }

    private HashMap<Integer, OpcodeMapValue[]> get_opcode_map_values() {
      HashMap<Integer, OpcodeMapValue[]> opcode_map_values = new HashMap<Integer, OpcodeMapValue[]>();
      opcode_map_values.put(0x05, new OpcodeMapValue[]{new OpcodeMapValue("add eax, ",false,"id")});
      opcode_map_values.put(0x01, new OpcodeMapValue[] {new OpcodeMapValue("add ", true, "mr")});
      opcode_map_values.put(0x03, new OpcodeMapValue[] {new OpcodeMapValue("add ", true, "rm")});
      opcode_map_values.put(0x81, new OpcodeMapValue[] {
          new OpcodeMapValue("add ", true, "mi"),
          new OpcodeMapValue("or ", true, "mi"),
          null,
          null,
          new OpcodeMapValue("and ", true, "mi"),
          new OpcodeMapValue("sub ", true, "mi"),
          new OpcodeMapValue("xor ", true, "mi")
      });
      opcode_map_values.put(0x31, new OpcodeMapValue[] {new OpcodeMapValue("xor ", true, "mr")});
      opcode_map_values.put(0xa9, new OpcodeMapValue[] {new OpcodeMapValue("test eax, ",false,"id")});
      opcode_map_values.put(0x85, new OpcodeMapValue[] {new OpcodeMapValue("test ", true, "mr")});
      opcode_map_values.put(0xf7, new OpcodeMapValue[] {
          new OpcodeMapValue("test ", true, "mi"),
          null,
          new OpcodeMapValue("not ", true, "m"),
          null,
          null,
          null,
          null,
          new OpcodeMapValue("idiv ", true, "m")
      });

      opcode_map_values.put(0x2d, new OpcodeMapValue[] {new OpcodeMapValue("sub eax, ",false,"id")});
      opcode_map_values.put(0x29, new OpcodeMapValue[] {new OpcodeMapValue("sub ", true, "mr")});
      opcode_map_values.put(0x2b, new OpcodeMapValue[] {new OpcodeMapValue("sub ", true, "rm")});
      opcode_map_values.put(0x8d, new OpcodeMapValue[] {new OpcodeMapValue("lea ", true, "rm")});
      opcode_map_values.put(0x25, new OpcodeMapValue[] {new OpcodeMapValue("and eax, ", false, "id")});
      opcode_map_values.put(0x21, new OpcodeMapValue[] {new OpcodeMapValue("and ", true, "mr")});
      opcode_map_values.put(0x23, new OpcodeMapValue[] {new OpcodeMapValue("and ", true, "rm")});
      opcode_map_values.put(0x0d, new OpcodeMapValue[] {new OpcodeMapValue("or eax, ", false, "id")});
      opcode_map_values.put(0xff, new OpcodeMapValue[] {
          new OpcodeMapValue("inc ", true, "m"),
          new OpcodeMapValue("dec ", true, "m"),
          new OpcodeMapValue("call ", true, "m"),
          null,
          new OpcodeMapValue("jmp ", true, "m"),
          null,
          new OpcodeMapValue("push ", true, "m")
      });
      opcode_map_values.put(0x09, new OpcodeMapValue[] {new OpcodeMapValue("or ", true, "mr")});
      opcode_map_values.put(0x0b, new OpcodeMapValue[] {new OpcodeMapValue("or ", true, "rm")});
      opcode_map_values.put(0x89, new OpcodeMapValue[] {new OpcodeMapValue("mov ", true, "mr")});
      opcode_map_values.put(0x8b, new OpcodeMapValue[] {new OpcodeMapValue("mov ", true, "rm")});
      opcode_map_values.put(0xc2, new OpcodeMapValue[] {new OpcodeMapValue("retn ", false, "i")});
      opcode_map_values.put(0xc3, new OpcodeMapValue[] {new OpcodeMapValue("retn ", false, "zo")});
      opcode_map_values.put(0x39, new OpcodeMapValue[] {new OpcodeMapValue("cmp ", true, "mr")});
      //Note: jz rel8 offset
      opcode_map_values.put(0x74, new OpcodeMapValue[] {new OpcodeMapValue("jz ", false, "cbd")});
      opcode_map_values.put(0x75, new OpcodeMapValue[] {new OpcodeMapValue("jnz ", false, "cbd")});
      opcode_map_values.put(0xeb, new OpcodeMapValue[] {new OpcodeMapValue("jmp ", false, "cbd")});
      opcode_map_values.put(0xe8, new OpcodeMapValue[] {new OpcodeMapValue("call ", false, "cd")});
      opcode_map_values.put(0xe9, new OpcodeMapValue[] {new OpcodeMapValue("jmp ", false, "cd")});


      //Note: For o Type Op Encoding (opcode + reg value)
      for(int i = 0; i < this.global_register_names.length; i++) {
         opcode_map_values.put(0x50 + i, new OpcodeMapValue[] {new OpcodeMapValue("push " + this.global_register_names[i], false, "o")});
         opcode_map_values.put(0x58 + i, new OpcodeMapValue[] {new OpcodeMapValue("pop " + this.global_register_names[i], false, "o")});
         opcode_map_values.put(0x48 + i, new OpcodeMapValue[] {new OpcodeMapValue("dec " + this.global_register_names[i], false, "o")});
         opcode_map_values.put(0x40 + i, new OpcodeMapValue[] {new OpcodeMapValue("inc " + this.global_register_names[i], false, "o")});
      }

      opcode_map_values.put(0x8f, new OpcodeMapValue[] {new OpcodeMapValue("pop ", true, "m")});
      opcode_map_values.put(0x68, new OpcodeMapValue[] {new OpcodeMapValue("push ", false, "id")});

      //Note: For oi Type Op Encoding (opcode + reg value)
      for(int i = 0; i < this.global_register_names.length; i++) {
         opcode_map_values.put(0xb8 + i, new OpcodeMapValue[] {new OpcodeMapValue("mov " + this.global_register_names[i] + ", ", false, "oi")});
      }
      return opcode_map_values;
    }

    public boolean isValidOpcode(int opcode) {
      return this.opcode_map_values.containsKey(opcode);
    }

    public long[] parseSIBByte(long SIBByte) {
        long scaleByte = ((SIBByte & 0xC0) >> 6);
        byte indexByte = (byte) ((SIBByte & 0x38) >> 3);
        byte baseByte = (byte) (SIBByte & 0x07);
        return new long[]{scaleByte, indexByte, baseByte};
    }

    public int[] parseModRM(int modRM) {
        int modByte = ((modRM & 0xC0) >> 6);
        byte regByte = (byte) ((modRM & 0x38) >> 3);
        byte rmByte = (byte) (modRM & 0x07);
        return new int[]{modByte, regByte, rmByte};
    }

    public String formatSIBInstructionString(String register, int scaleMultiplier, String baseRegister, int displacementBytes) {
        if(displacementBytes > 0) {
          InstructionValues instructionValues = this.getLittleEndianAddr(displacementBytes);
          this.instructionValues.instructionBytes += String.format("%s ", instructionValues.instructionBytes);
          if(baseRegister.length() > 0) {
            return String.format("[ %s*%d + %s + %s ]", register, scaleMultiplier, baseRegister, instructionValues.instruction);
          }
          return String.format("[ %s*%d + %s ]", register, scaleMultiplier, instructionValues.instruction);
        } else {
          if(baseRegister.length() > 0) {
            return String.format("[ %s*%d + %s ]", register, scaleMultiplier, baseRegister);
          }
          return String.format("[ %s*%d ]", register, scaleMultiplier);
        }
    }

    public String getSIBInstruction(int modBits, int displacementBytes) {
      long[] SIBByte = this.parseSIBByte(this.buffer.get((int) this.index));
      long scaleBits = SIBByte[0];
      long indexBits = SIBByte[1];
      long baseBits = SIBByte[2];
      this.index++;
      String instruction = this.formatSIBInstructionString(this.global_register_names[(int) indexBits], (int) Math.pow(2, scaleBits), modBits == 0b00 && baseBits == 0b101 ? "" : this.global_register_names[(int) baseBits], displacementBytes);
      this.index += displacementBytes;
      return instruction;
    }

    public void printDisasm() {
        ArrayList<String> sortedList = new ArrayList<String>(this.outputList.keySet());
        Collections.sort(sortedList);
        for(String key : sortedList) {
          if (this.labelMap.get(key) != null) {
            System.out.println(this.labelMap.get(key) + ":");
            this.labelMap.remove(key);
          }
          String output = this.outputList.get(key);
          System.out.println(key + ": " + output);
        }
        ArrayList<String> sortedLabelList = new ArrayList<String>(this.labelMap.keySet());
        Collections.sort(sortedLabelList);
        for (String key : sortedLabelList) {
          System.out.println(this.labelMap.get(key) + ":");
        }
    }

    public InstructionValues getLittleEndianAddr(int bytes) {
        StringBuilder address = new StringBuilder();
        StringBuilder instructionBytes = new StringBuilder();
        long start = this.index + (bytes - 1);
        long end = this.index;
        long instructionByteIndex = this.index;
        for(long i = start; i >= end; i--) {
            try {
              instructionBytes.append(String.format("%02X ", this.buffer.get((int) instructionByteIndex)));
              instructionByteIndex++;
              address.append(String.format("%02X", this.buffer.get((int) i)));
            } catch (IndexOutOfBoundsException e) {
              e.printStackTrace();
              return new InstructionValues("", "");
            }
        }
        return new InstructionValues(instructionBytes.toString(), String.format("0x%s", address.toString()));
    }

    public ArrayList<Long> getBuffer(String fileName) {
      try {
        InputStream input = new FileInputStream(fileName);
        int byteBuffer;
        ArrayList<Long> bytes = new ArrayList<>();
        while((byteBuffer = input.read()) != -1) {
          bytes.add((long) byteBuffer);
        }
        return bytes;
      } catch(IOException e) {
        e.printStackTrace();
      }
      return new ArrayList<>();
    }

    private String getRMInstruction(int rm, int mod) {
        String instruction;
        if(mod == 3) {
            System.out.println("r/m32 operand is direct register");
            return this.global_register_names[rm];
        } else if(mod == 2) {
          if(rm == 4) {
            // SIB Byte
            return this.getSIBInstruction(mod, 4);
          } else {
            System.out.println("r/m32 operand is [ reg + disp32 ]");
            InstructionValues instructionValues = (this.getLittleEndianAddr(4));
            this.instructionValues.instructionBytes += instructionValues.instructionBytes;
            instruction = String.format("[ %s + %s ]", this.global_register_names[rm], instructionValues.instruction);
            this.index += 4;
            return instruction;
          }
        } else if(mod == 1) {
          System.out.println("r/m32 operand is [ reg + disp8 ]");
          if(rm == 4) {
            // SIB Byte
            return this.getSIBInstruction(mod, 1);
          }
          InstructionValues instructionValues = this.getLittleEndianAddr(1);
          this.instructionValues.instructionBytes += String.format("%s ", instructionValues.instructionBytes);
          instruction = String.format("[ %s + %s ]", this.global_register_names[rm], instructionValues.instruction);
          this.index += 1;
          return instruction;
        } else if(mod == 0) {
          if (rm == 5) {
            System.out.println("r/m32 operand is [ disp32 ]");
            InstructionValues instructionValues = this.getLittleEndianAddr(4);
            this.instructionValues.instructionBytes += String.format("%s ", instructionValues.instructionBytes);
            instruction = String.format("[ %s ]", instructionValues.instruction);
            this.index += 4;
            return instruction;
          } else if(rm == 4) {
            // SIB Byte
            return this.getSIBInstruction(mod, 0);
          } else {
              System.out.println("r/m32 operand is [reg]");
              return String.format("[ %s ]", this.global_register_names[rm]);
          }
        }
        return "";
    }

    private String buildFullModRMInstructionByCommandType(OpcodeMapValue opcodeMapValue, String rmInstruction, String regInstruction) {
      String instruction = opcodeMapValue.command;
      if (Objects.equals(opcodeMapValue.commandType,"mr")) {
        return String.format("%s%s, %s", instruction, rmInstruction, regInstruction);
      } else if (Objects.equals(opcodeMapValue.commandType, "rm")) {
        return String.format("%s%s, %s", instruction, regInstruction, rmInstruction);
      } else if (Objects.equals(opcodeMapValue.commandType, "mi")) {
        // This line needs to be before the this.index += 4 increment because we utilize this.index in getLittleEndianAddr()
        InstructionValues instructionValues = this.getLittleEndianAddr(4);
        this.instructionValues.instructionBytes += String.format("%s ", instructionValues.instructionBytes);
        this.index += 4;
        return String.format("%s%s, %s", instruction, rmInstruction, instructionValues.instruction);
      } else if (Objects.equals(opcodeMapValue.commandType, "m")) {
        return String.format("%s%s", instruction, rmInstruction);
      }

      return "";
    }

    private void buildNonModRMInstruction(OpcodeMapValue opcodeMapValue) {
      if(Objects.equals(opcodeMapValue.commandType, "o")) {
          this.instructionValues.instruction += opcodeMapValue.command;
      } else if(Objects.equals(opcodeMapValue.commandType, "oi")) {
          this.instructionValues.instruction += opcodeMapValue.command + this.getLittleEndianAddr( 4).instruction;
          this.index += 4;
      } else if(Objects.equals(opcodeMapValue.commandType, "i")) {
          this.instructionValues.instruction += opcodeMapValue.command + this.getLittleEndianAddr( 2).instruction;
          this.index += 2;
      } else if(Objects.equals(opcodeMapValue.commandType, "zo")) {
          this.instructionValues.instruction += opcodeMapValue.command;
      } else if(Objects.equals(opcodeMapValue.commandType, "cbd")) {
          InstructionValues offsetAddressInstructionValues = this.getLittleEndianAddr(1);
          // 1 byte opcode + 1 byte displacement = 2 bytes, which we use to calculate the address of the label
          // Address of label: Take rel8 displacement, treating it as a sign extended 32-bit value.
          // Add up displacement + current address + instruction bytes - overflow value = Address
          int sizeOfInstruction = 2;
          int decodedInstruction = Integer.decode(offsetAddressInstructionValues.instruction);
          int labelAddr = decodedInstruction + sizeOfInstruction + (int) instructionStartIndex;
          String labelAddrStr = String.format("%01x", decodedInstruction + sizeOfInstruction + instructionStartIndex);
          if(labelAddr > this.buffer.size()) {
            // Remove overflow if necessary
            labelAddrStr = labelAddrStr.substring(1);
            labelAddr = Integer.decode(String.format("0x%s", labelAddrStr));
          }
          String label;

          if (this.labelMap.get(String.format("%08x", labelAddr)) != null) {
            label = this.labelMap.get(String.format("%08x", labelAddr));
          } else {
            label = String.format("offset_%08x", labelAddr);
            this.labelMap.put(String.format("%08x", labelAddr), label);
          }

          this.instructionValues.instruction += opcodeMapValue.command + label;
          this.instructionValues.instructionBytes = String.format("%s%s ", this.instructionValues.instructionBytes, offsetAddressInstructionValues.instructionBytes);
          this.index++;
      } else if (Objects.equals(opcodeMapValue.commandType, "cb")) {
          InstructionValues offsetAddressInstructionValues = this.getLittleEndianAddr(1);
          int sizeOfInstruction = 2;
          int labelAddr = Integer.decode(offsetAddressInstructionValues.instruction) + (int) instructionStartIndex + sizeOfInstruction;
          String label;
          if (this.labelMap.get(String.format("%08x", labelAddr)) != null) {
            label = this.labelMap.get(String.format("%08x", labelAddr));
          } else {
            label = String.format("offset_%08x", labelAddr);
            this.labelMap.put(String.format("%08x", labelAddr), label);
          }

          this.instructionValues.instruction += opcodeMapValue.command + label;
          this.instructionValues.instructionBytes = String.format("%s%s ", this.instructionValues.instructionBytes, offsetAddressInstructionValues.instructionBytes);
          this.index++;
      } else if(Objects.equals(opcodeMapValue.commandType, "cd")) {
          InstructionValues offsetAddressInstructionValues = this.getLittleEndianAddr(4);
          // 1 byte opcode + 4 byte displacement = 5 bytes
          int sizeOfInstruction = 5;
          Long labelAddr = this.instructionStartIndex + Long.decode(String.format("%s", offsetAddressInstructionValues.instruction)) + sizeOfInstruction;
          String label;
          if (this.labelMap.get(String.format("%08x", labelAddr)) != null) {
            label = this.labelMap.get(String.format("%08x", labelAddr));
          } else {
            label = String.format("offset_%08x", labelAddr);
            this.labelMap.put(String.format("%08x", labelAddr), label);
          }
          this.instructionValues.instruction = String.format("%s%s%s", this.instructionValues.instruction, opcodeMapValue.command, label);
          this.instructionValues.instructionBytes = String.format("%s%s", this.instructionValues.instructionBytes, offsetAddressInstructionValues.instructionBytes);
          this.index += 4;
      } else if(Objects.equals(opcodeMapValue.commandType, "id")) {
          InstructionValues instructionValues = this.getLittleEndianAddr(4);
          this.instructionValues.instruction = String.format("%s%s%s", this.instructionValues.instruction, opcodeMapValue.command, instructionValues.instruction);
          this.instructionValues.instructionBytes = String.format("%s%s", this.instructionValues.instructionBytes, instructionValues.instructionBytes);
          this.index += 4;
      }
    }

    public void disassemble(String file) {
      this.buffer = this.getBuffer(file);
      this.index = 0;
      this.instructionStartIndex = 0;
      while(this.index < this.buffer.size()) {
        long opcode = this.buffer.get((int) this.index);
        if (this.instructionStartIndex < this.index) {
          this.instructionStartIndex = this.index;
        }
          this.index += 1;
          if (this.index > this.buffer.size()) {
            break;
          }
          if (isValidOpcode((int) opcode)) {
            System.out.println("Found valid opcode");
            this.instructionValues = new InstructionValues(String.format("%02X ", opcode), "");
            System.out.println("instruction_bytes: " + String.format("%02x", opcode));
            OpcodeMapValue[] opcodeMapValues = this.opcode_map_values.get((int) opcode);
            System.out.println("Index -> " + this.index);
            if (opcodeMapValues.length > 1 || (opcodeMapValues.length == 1 && opcodeMapValues[0].requiresModRMByte)) {
              System.out.println("REQUIRES MODRM BYTE");
              long modrm = this.buffer.get((int) this.index);
              this.instructionValues.instructionBytes += String.format("%02x ", modrm);
              this.index += 1;
              int[] modRMBytes = this.parseModRM((int) modrm);
              int mod = modRMBytes[0];
              int reg = modRMBytes[1];
              int rm = modRMBytes[2];
              String rmInstruction = this.getRMInstruction(rm, mod);
              String regInstruction = this.global_register_names[reg];
              OpcodeMapValue opcodeMapValue = null;
              if(opcodeMapValues.length == 1) {
                opcodeMapValue = opcodeMapValues[0];
              } else if(opcodeMapValues.length >= reg + 1) {
                opcodeMapValue = opcodeMapValues[reg];
              }
              if (opcodeMapValue != null && rmInstruction.length() > 0) {
                this.instructionValues.instruction += this.buildFullModRMInstructionByCommandType(opcodeMapValue, rmInstruction, regInstruction);
                System.out.println("Adding to list " + this.instructionValues.instruction);
                outputList.put(String.format("%08x", this.instructionStartIndex), this.instructionValues.instructionBytes + this.instructionValues.instruction);
              } else {
                outputList.put(String.format("%08x", this.instructionStartIndex), String.format("%02x db 0x%02x", opcode & 0xff, opcode & 0xff));
              }
            } else {
              System.out.println("Does not require MODRM - modify to complete the instruction and consume the appropriate bytes");
              OpcodeMapValue opcodeMapValue = opcodeMapValues[0];
              this.buildNonModRMInstruction(opcodeMapValue);
              if (this.instructionValues.instruction.length() > 0) {
                this.outputList.put(String.format("%08x", this.instructionStartIndex), this.instructionValues.instructionBytes + this.instructionValues.instruction);
              } else {
                outputList.put(String.format("%08x", this.instructionStartIndex), String.format("%02x db 0x%02x", opcode & 0xff, opcode & 0xff));
              }
            }
          } else {
            // NOT A VALID OPCODE!
            outputList.put(String.format("%08x", this.instructionStartIndex), String.format("%02x db 0x%02x", opcode & 0xff, opcode & 0xff));
          }
      }

      this.printDisasm();
    }
}
