/* LanguageTool, a natural language style checker
 * Copyright (C) 2014 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.ar;

import org.jetbrains.annotations.Nullable;
import org.languagetool.AnalyzedToken;
import org.languagetool.Language;
import org.languagetool.rules.Example;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.morfologik.MorfologikSpellerRule;
import org.languagetool.synthesis.ar.ArabicSynthesizer;

import java.io.IOException;
import java.util.*;

public abstract class AbstractArabicSpellerRule extends MorfologikSpellerRule {

  private final ArabicSynthesizer synthesizer = new ArabicSynthesizer();

  public AbstractArabicSpellerRule(ResourceBundle messages, Language language) throws IOException {
    super(messages, language);
    setCheckCompound(true);
    addExamplePair(Example.wrong("هذه <marker>العباره</marker> فيها أغلاط."),
                   Example.fixed("هذه <marker>العبارة</marker> فيها أغلاط."));
  }

  @Override
  protected List<RuleMatch> getRuleMatches(String word, int startPos) throws IOException {
    List<RuleMatch> ruleMatches = super.getRuleMatches(word, startPos);
    if (ruleMatches.size() > 0) {
      // so 'word' is misspelled: 
      IrregularForms forms = getIrregularFormsOrNull(word);
      if (forms != null) {
        RuleMatch oldMatch = ruleMatches.get(0);
        RuleMatch newMatch = new RuleMatch(this, oldMatch.getFromPos(), oldMatch.getToPos(), 
                "Possible spelling mistake. Did you mean <suggestion>" + forms.forms.get(0) +
                "</suggestion>, the irregular " + forms.formName + " form of the " + forms.posName +
                " '" + forms.baseform + "'?");
        List<String> allSuggestions = new ArrayList<>();
        allSuggestions.addAll(forms.forms);
        for (String repl : oldMatch.getSuggestedReplacements()) {
          if (!allSuggestions.contains(repl)) {
            allSuggestions.add(repl);
          }
        }
        newMatch.setSuggestedReplacements(allSuggestions);
        ruleMatches.set(0, newMatch);
      }
    }
    return ruleMatches;
  }

  @SuppressWarnings({"ReuseOfLocalVariable", "ControlFlowStatementWithoutBraces"})
  @Nullable
  private IrregularForms getIrregularFormsOrNull(String word) {
    IrregularForms irregularFormsOrNull = getIrregularFormsOrNull(word, "ed", Arrays.asList("ed"), "VBD", "verb", "past tense");
    return irregularFormsOrNull;
  }

  @Nullable
  private IrregularForms getIrregularFormsOrNull(String word, String wordSuffix, List<String> suffixes, String posTag, String posName, String formName) {
    try {
      for (String suffix : suffixes) {
        if (word.endsWith(wordSuffix)) {
          String baseForm = word.substring(0, word.length() - suffix.length());
          String[] forms = synthesizer.synthesize(new AnalyzedToken(word, null, baseForm), posTag);
          List<String> result = new ArrayList<>();
          for (String form : forms) {
            if (!speller1.isMisspelled(form)) {
              // only accept suggestions that the spellchecker will accept
              result.add(form);
            }
          }
          // the internal dict might contain forms that the spell checker doesn't accept (e.g. 'criterions'),
          // but we trust the spell checker in this case:
          result.remove(word);
         // result.remove("badder");  // non-standard usage
         // result.remove("baddest");  // non-standard usage
         // result.remove("spake");  // can be removed after dict update
          if (result.size() > 0) {
            return new IrregularForms(baseForm, posName, formName, result);
          }
        }
      }
      return null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @throws IOException 
   * @since 2.7
   */
  @Override
  protected List<String> getAdditionalTopSuggestions(List<String> suggestions, String word) throws IOException {
    if ("Alot".equals(word)) {
      return Arrays.asList("A lot");
    } else if ("alot".equals(word)) {
      return Arrays.asList("a lot");
    } 
    return super.getAdditionalTopSuggestions(suggestions, word);
  }

  private static class IrregularForms {
    final String baseform;
    final String posName;
    final String formName;
    final List<String> forms;
    private IrregularForms(String baseform, String posName, String formName, List<String> forms) {
      this.baseform = baseform;
      this.posName = posName;
      this.formName = formName;
      this.forms = forms;
    }
  }
}