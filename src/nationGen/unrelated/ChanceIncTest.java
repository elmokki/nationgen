package nationGen.unrelated;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;



public class ChanceIncTest {

	public static void main(String[] args) throws IOException {

		new ChanceIncTest();
		
		
		
	}
	
	public ChanceIncTest()
	{

		System.out.println("1: " + checkChanceInc("true") + " / true");
		System.out.println("2: " + checkChanceInc("not true") + " / false");
		System.out.println("3: " + checkChanceInc("false") + " / false");
		System.out.println("4: " + checkChanceInc("not false") + " / true");
		System.out.println("5: " + checkChanceInc("false or true") + " / true");
		System.out.println("6: " + checkChanceInc("false and true") + " / false");
		System.out.println("7: " + checkChanceInc("true and not false") + " / true");
		System.out.println("8: " + checkChanceInc("not false and true") + " / true");




	}
	
	
	public boolean checkChanceInc(String s)
	{
		List<String> strl = partitionChanceInc(s);
		System.out.println(strl);
		return validateChanceInc(strl);
	}
	
	public List<String> partitionChanceInc(String str)
	{
		List<String> stuff = Generic.parseArgs(str);
		List<String> chanceincs = new ArrayList<String>();
		
		String con = "";
		boolean par = false;
		int open = 0;
		for(String s : stuff)
		{
			if(par)
			{
				if(s.endsWith(")"))
				{
					open--;
					if(open == 0)
					{
						par = false;
						con = (con + " " + s.substring(0,s.length()-1)).trim();
					}
					else
					{
						con = (con + " " + s).trim();
					}
				}
				else
				{
					con = (con + " " + s).trim();
				}
			}
			else
			{
				if(s.startsWith("("))
				{
					open++;
					if(open == 1)
					{
						par = true;
						con = (con + " " + s.substring(1)).trim();
					}
					else
					{
						con = (con + " " + s).trim();
					}
				}
				else if(s.equals("and") || s.equals("or"))
				{
					chanceincs.add(con);
					chanceincs.add(s);
					con = "";
				}
				else
				{
					con = (con + " " + s).trim();
				}
			}
		}
		if(con.length() > 0)
			chanceincs.add(con);

		return chanceincs;
	}
	
	

	private boolean fixNullBoolean(Boolean success, String str)
	{
		boolean not = Generic.parseArgs(str).get(0).equals("not");
		
		if(success == null)
		{
			return false;
		}
		else if(success != not)
		{
			return true;
		}
		return false;
	}
	public boolean validateChanceInc(List<String> chanceincs)
	{

		
		boolean istrue = false;
		String operator = "";
		
		for(String s : chanceincs)
		{
			
			// If it used to be within (meaningful) parentheses we solve it separately
			if(!s.equals("or") && !s.equals("and") && (s.contains(" or ") || s.contains(" and ")))
			{

				List<String> tincs = partitionChanceInc(s);
				boolean check = validateChanceInc(tincs);
				 
				if(check)
					s = "true";
				else
					s = "false";
				
			}
			
			
			
			// If it is a logical operator
			if(s.equals("or") || s.equals("and"))
			{
				operator = s;
			}
			// If it is normal chanceinc
			else
			{
				System.out.println("Test: " + s + " -> " + fixNullBoolean(check(s), s));
				if(operator.equals(""))
				{
					istrue = fixNullBoolean(check(s), s);
				}
				else
				{
					if(operator.equals("or"))
					{
						istrue = istrue || fixNullBoolean(check(s), s);
					}
					else if(operator.equals("and"))
					{
						istrue = istrue && fixNullBoolean(check(s), s);
					}
					operator = "";
				}
				

			}

		}
	
		return istrue;
	}
	
	
	private boolean check(String s)
	{
		System.out.print("Checking " + s + " ");
		if(s.startsWith("not "))
			s = s.substring(4);
			
		boolean ok = false;
		if(s.equals("true"))
			ok = true;
		
		System.out.println("-> " + s + " -> " + ok);
		return ok;

	}

}
