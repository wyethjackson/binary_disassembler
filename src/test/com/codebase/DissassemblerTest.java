package test.com.codebase;

import com.codebase.Dissassembler;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

/** 
* Dissassembler Tester. 
* 
* @author <Authors name> 
* @since <pre>Sep 25, 2022</pre> 
* @version 1.0 
*/ 
public class DissassemblerTest { 

@Before
public void before() throws Exception { 
} 

@After
public void after() throws Exception { 
} 


/** 
* 
* Method: get_opcode_map_values() 
* 
*/ 
@Test
public void testGet_opcode_map_values() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = Dissassembler.getClass().getMethod("get_opcode_map_values"); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: isValidOpcode(byte opcode) 
* 
*/ 
@Test
public void testIsValidOpcode() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = Dissassembler.getClass().getMethod("isValidOpcode", byte.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: parseModRM(byte modRM) 
* 
*/ 
@Test
public void testParseModRM() throws Exception {
    int modRM = 0b11001000;
    Dissassembler dissassembler = new Dissassembler();
    int[] parsedBytes = dissassembler.parseModRM(modRM);
    assertEquals(parsedBytes[0], 0b11);
    assertEquals(parsedBytes[1], (byte) 0b001);
    assertEquals(parsedBytes[2], (byte) 0b000);
//TODO: Test goes here... 
/* 
try { 
   Method method = Dissassembler.getClass().getMethod("parseModRM", byte.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

} 
