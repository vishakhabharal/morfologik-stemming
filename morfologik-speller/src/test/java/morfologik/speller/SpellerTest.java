package morfologik.speller;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import morfologik.speller.Speller;
import morfologik.stemming.Dictionary;

import org.junit.BeforeClass;
import org.junit.Test;

public class SpellerTest {
    private static Dictionary slownikDictionary;

    @BeforeClass
    public static void setup() throws Exception {
        final URL url = SpellerTest.class.getResource("slownik.dict");     
        slownikDictionary = Dictionary.read(url);
    }

    
    @Test
    public void testAbka() throws Exception {
        final Speller spell = new Speller(slownikDictionary, 2);
        System.out.println("Replacements:");
        for (String s : spell.findReplacements("abka")) {
            System.out.println(s);
        }
    }
    
	@Test
	public void testRunonWords() throws IOException {
		final Speller spell = new Speller(slownikDictionary);
		assertTrue(spell.replaceRunOnWords("abaka").isEmpty());
		assertTrue(spell.replaceRunOnWords("abakaabace").
				contains("abaka abace"));

	}
	
	@Test
	public void testInfixedNotSupported() {    
       // Test on an morphological dictionary - should NOT work as well
        final URL url1 = getClass().getResource("test-infix.dict");     
        Speller spell1;
        try {
            spell1 = new Speller(Dictionary.read(url1));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);            
        } catch (IOException e) {
            fail("File test-infix.dict not found!");
        }
	}
	
	@Test
	public void testFindReplacements() throws IOException {
		final Speller spell = new Speller(slownikDictionary, 1);
		assertTrue(spell.findReplacements("abka").contains("abak"));
	      //check if we get only dictionary words...
		List<String> reps = spell.findReplacements("bak");
		    for (final String word: reps) {
		        assertTrue(spell.isInDictionary(word));
		    }
		assertTrue(spell.findReplacements("abka~~").isEmpty()); // 2 characters more -> edit distance too large
		assertTrue(!spell.findReplacements("Rezkunia").contains("Rzekunia"));
				
		//diacritics
		assertTrue(spell.findReplacements("ąbak").contains("abak"));
		//we should get no candidates for correct words
		assertTrue(spell.isInDictionary("abak"));
		assertTrue(spell.findReplacements("abako").isEmpty());
		//and no for things that are too different from the dictionary
		assertTrue(spell.findReplacements("Strefakibica").isEmpty());
		//nothing for nothing
		assertTrue(spell.findReplacements("").isEmpty());
	    //nothing for weird characters
        assertTrue(spell.findReplacements("\u0000").isEmpty());
        //nothing for other characters
        assertTrue(spell.findReplacements("«…»").isEmpty());
        //nothing for separator
        assertTrue(spell.findReplacements("+").isEmpty());

	}
	
	@Test
	public void testEditDistanceCalculation() throws IOException {
        final Speller spell = new Speller(slownikDictionary, 5);
        //test examples from Oflazer's paper
	    assertTrue(getEditDistance(spell, "recoginze", "recognize") == 1);
	    assertTrue(getEditDistance(spell, "sailn", "failing") == 3);
	    assertTrue(getEditDistance(spell, "abc", "abcd") == 1);	    
	    assertTrue(getEditDistance(spell, "abc", "abcde") == 2);
	    //test words from fsa_spell output
	    assertTrue(getEditDistance(spell, "abka", "abaka") == 1);
	    assertTrue(getEditDistance(spell, "abka", "abakan") == 2);
	    assertTrue(getEditDistance(spell, "abka", "abaką") == 2);
	    assertTrue(getEditDistance(spell, "abka", "abaki") == 2);
	}
	
	@Test
	public void testCutOffEditDistance() throws IOException {
	    final Speller spell2 = new Speller(slownikDictionary, 2); //note: threshold = 2        
        //test cut edit distance - reprter / repo from Oflazer	    
        assertTrue(getCutOffDistance(spell2, "repo", "reprter") == 1);
        assertTrue(getCutOffDistance(spell2, "reporter", "reporter") == 0);
	}
	
	private int getCutOffDistance(final Speller spell, final String word, final String candidate) {
	    spell.setWordAndCandidate(word, candidate);
        final int [] ced = new int[spell.getCandLen() - spell.getWordLen()];
        for (int i = 0; i < spell.getCandLen() - spell.getWordLen(); i++) {
            ced[i] = spell.cuted(spell.getWordLen() + i);
        }
        Arrays.sort(ced);
        //and the min value...
        if (ced.length > 0) {
            return ced[0];
        }
        return 0;
	}
	
	private int getEditDistance(final Speller spell, final String word, final String candidate) {
	    spell.setWordAndCandidate(word, candidate);	    	   	    
	    final int maxDistance = spell.getEffectiveED(); 
	    final int candidateLen = spell.getCandLen();
	    final int wordLen = spell.getWordLen();
	    int ed = 0;
	    for (int i = 0; i < candidateLen; i++) {
	        if (spell.cuted(i) <= maxDistance) {
	            if (Math.abs(wordLen - 1 - i) <= maxDistance) {
	                ed = spell.ed(wordLen - 1, i);
	            }
	        } 
	    }
	    return ed;
	}
}