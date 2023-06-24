package com.codebase;

public class Main {

    public static void main(String[] args) {
      try {
	      Dissassembler dissassembler = new Dissassembler();
        if(args.length < 1) {
          System.out.println("Please enter filename.");
          System.exit(0);
        }
        dissassembler.disassemble(args[0]);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
}
