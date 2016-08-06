/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.memory;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.tuke.emustudio.rasp.memory.gui.MemoryWindow;

/**
 *
 * @author miso
 */
public class MockProgramCreator {

    /**
     * Constructs mock compiler output, it is a program taking a natural number,
     * output is its factorial.
     */
    private static void factorialProgramCreate() {
        //program as array of items
        MemoryItem[] memoryItems = new MemoryItem[]{
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(1),
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(2),
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(3),
            new RASPInstructionImpl(RASPInstruction.READ),
            new NumberMemoryItem(1),
            new RASPInstructionImpl(RASPInstruction.LOAD_REGISTER),
            new NumberMemoryItem(1),
            new RASPInstructionImpl(RASPInstruction.JGTZ),
            new NumberMemoryItem(19),
            new RASPInstructionImpl(RASPInstruction.JMP),
            new NumberMemoryItem(37),
            new RASPInstructionImpl(RASPInstruction.LOAD_REGISTER),
            new NumberMemoryItem(3),
            new RASPInstructionImpl(RASPInstruction.SUB_REGISTER),
            new NumberMemoryItem(1),
            new RASPInstructionImpl(RASPInstruction.JZ),
            new NumberMemoryItem(37),
            new RASPInstructionImpl(RASPInstruction.LOAD_REGISTER),
            new NumberMemoryItem(3),
            new RASPInstructionImpl(RASPInstruction.ADD_CONSTANT),
            new NumberMemoryItem(1),
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(3),
            new RASPInstructionImpl(RASPInstruction.MUL_REGISTER),
            new NumberMemoryItem(2),
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(2),
            new RASPInstructionImpl(RASPInstruction.JMP),
            new NumberMemoryItem(19),
            new RASPInstructionImpl(RASPInstruction.WRITE_REGISTER),
            new NumberMemoryItem(2),
            //==========================================================
            //testing if writing beyond program area works fine
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(25),
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(60),
            //==========================================================

            //terminating execution
            new RASPInstructionImpl(RASPInstruction.HALT),
            new NumberMemoryItem(0)
        };

        //construct HashMap with labels
        HashMap<Integer, String> labels = new HashMap<>();
        labels.put(19, "OK");
        labels.put(37, "FINISH");

        //prepare program start attribute
        Integer programStart = 5;

        //construct list with memory items
        ArrayList<MemoryItem> memory = new ArrayList<>(Arrays.asList(memoryItems));

        //save program to file
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("factorial.bin");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
                objectOutputStream.writeObject(labels);
                objectOutputStream.writeObject(programStart);
                objectOutputStream.writeObject(memory);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Constructs mock compiler output, this program demonstrates instruction
     * modification.
     */
    private static void instructModifProgramCreate() {
        MemoryItem[] memoryItems = new MemoryItem[]{
            //testing instruction modification
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(32), //loads constant 32 to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(1), //stores 32 to R1
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(30), //loads constant 30 to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(2), //stores 30 to R2
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(8), //load ADD_REGISTER instruction (opcode 8)
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(19), //replace SUB_REGISTER by ADD_REGISTER
            new RASPInstructionImpl(RASPInstruction.LOAD_REGISTER),
            new NumberMemoryItem(1), //loads 32 (R1) to ACC
            new RASPInstructionImpl(RASPInstruction.SUB_REGISTER),
            new NumberMemoryItem(2),
            new RASPInstructionImpl(RASPInstruction.HALT),
            new NumberMemoryItem(0)
        };

        //construct HashMap with labels
        HashMap<Integer, String> labels = new HashMap<>(); //no labels, so empty map
        //prepare program start
        Integer programStart = 5;
        //prepare memory content
        ArrayList<MemoryItem> memory = new ArrayList<>(Arrays.asList(memoryItems));

        //save program to file
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("instr_modif.bin");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
                objectOutputStream.writeObject(labels);
                objectOutputStream.writeObject(programStart);
                objectOutputStream.writeObject(memory);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * The same program as the previous one, but without instruction
     * modification, just to test it works.
     */
    private static void instrModifProgramWithoutModificationCreate() {
        MemoryItem[] memoryItems = new MemoryItem[]{
            //testing instruction modification
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(32), //loads constant 32 to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(1), //stores 32 to R1
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(30), //loads constant 30 to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(2), //stores 30 to R2
            new RASPInstructionImpl(RASPInstruction.LOAD_REGISTER),
            new NumberMemoryItem(1), //loads 32 (R1) to ACC
            new RASPInstructionImpl(RASPInstruction.SUB_REGISTER),
            new NumberMemoryItem(2),
            new RASPInstructionImpl(RASPInstruction.HALT),
            new NumberMemoryItem(0)
        };

        //construct HashMap with labels
        HashMap<Integer, String> labels = new HashMap<>(); //no labels, so empty map
        //prepare program start
        Integer programStart = 5;
        //prepare memory content
        ArrayList<MemoryItem> memory = new ArrayList<>(Arrays.asList(memoryItems));

        //save program to file
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("instr_without_modif.bin");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
                objectOutputStream.writeObject(labels);
                objectOutputStream.writeObject(programStart);
                objectOutputStream.writeObject(memory);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Constructs mock compiler output, this program demonstrates operand
     * modification.
     */
    private static void operandModifProgramCreate() {
        MemoryItem[] memoryItems = new MemoryItem[]{
            //testing instruction modification
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(32), //loads constant 32 to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(1), //stores 32 to R1
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(30), //loads constant 30 to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(2), //stores 30 to R2
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(16), //loads 16 constant to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(3), //stores 16 to R3
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(3), //load 3 to ACC 
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(24), //modifies operand            
            new RASPInstructionImpl(RASPInstruction.LOAD_REGISTER),
            new NumberMemoryItem(1), //loads 32 (R1) to ACC            
            new RASPInstructionImpl(RASPInstruction.SUB_REGISTER),
            new NumberMemoryItem(2),
            new RASPInstructionImpl(RASPInstruction.HALT),
            new NumberMemoryItem(0)
        };

        //construct HashMap with labels
        HashMap<Integer, String> labels = new HashMap<>(); //no labels, so empty map
        //prepare program start
        Integer programStart = 5;
        //prepare memory content
        ArrayList<MemoryItem> memory = new ArrayList<>(Arrays.asList(memoryItems));

        //save program to file
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("operand_modif.bin");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
                objectOutputStream.writeObject(labels);
                objectOutputStream.writeObject(programStart);
                objectOutputStream.writeObject(memory);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * The same as previous, but without operand modification, to be sure all
     * works.
     */
    private static void operandModifWithoutModifProgramCreate() {
        MemoryItem[] memoryItems = new MemoryItem[]{
            //testing instruction modification
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(32), //loads constant 32 to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(1), //stores 32 to R1
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(30), //loads constant 30 to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(2), //stores 30 to R2
            new RASPInstructionImpl(RASPInstruction.LOAD_CONSTANT),
            new NumberMemoryItem(16), //loads 16 constant to ACC
            new RASPInstructionImpl(RASPInstruction.STORE),
            new NumberMemoryItem(3), //stores 16 to R3
            new RASPInstructionImpl(RASPInstruction.LOAD_REGISTER),
            new NumberMemoryItem(1), //loads 32 (R1) to ACC            
            new RASPInstructionImpl(RASPInstruction.SUB_REGISTER),
            new NumberMemoryItem(2),
            new RASPInstructionImpl(RASPInstruction.HALT),
            new NumberMemoryItem(0)
        };

        //construct HashMap with labels
        HashMap<Integer, String> labels = new HashMap<>(); //no labels, so empty map
        //prepare program start
        Integer programStart = 5;
        //prepare memory content
        ArrayList<MemoryItem> memory = new ArrayList<>(Arrays.asList(memoryItems));

        //save program to file
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("operand_without_modif.bin");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
                objectOutputStream.writeObject(labels);
                objectOutputStream.writeObject(programStart);
                objectOutputStream.writeObject(memory);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        factorialProgramCreate();
        instructModifProgramCreate();
        instrModifProgramWithoutModificationCreate();
        operandModifProgramCreate();
        operandModifWithoutModifProgramCreate();
    }

}
