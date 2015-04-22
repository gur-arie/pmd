/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

/**
 * Defines the Language module for Apex (Salesforce)
 * @author Ariel Gorfinkel, Novidea Software
 */
public class ApexLanguage extends AbstractLanguage {

	/**
     * Creates a new instance of {@link ApexLanguage} with the default extensions for Apex files.
     */
	public ApexLanguage(){
		super("Apex", "apex", new ApexTokenizer(), ".cls", ".trigger");
	}
}