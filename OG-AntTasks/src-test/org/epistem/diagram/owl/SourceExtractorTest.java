/*--------------------------------------------------------------------------------
  Copyright (c) 2011, David N. Main
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--------------------------------------------------------------------------------*/
/**
 * 
 */
package org.epistem.diagram.owl;

import java.io.File;
import java.util.Map;

import junit.framework.TestCase;

import org.epistem.diagram.literate.SourceFileExtractor;
import org.epistem.diagram.model.Diagram;
import org.epistem.graffle.OmniGraffleDoc;

/**
 * Test the source extractor
 *
 * @author nickmain
 */
public class SourceExtractorTest extends TestCase {

    
    public void testExtractor() throws Exception {
        SourceFileExtractor extractor = new SourceFileExtractor();
        
        OmniGraffleDoc doc = new OmniGraffleDoc( new File( "test-diagrams/test-src-extract.graffle" ) );
        Diagram diagram = new Diagram( doc );          
        diagram.accept( extractor );
        
        Map<String, String> srcs = extractor.getFileSources();
        assertEquals( srcs.get( "../src/bar.scm" ), "this is a test AA\nthis is a test 11\nthis is a\ntest A\nthis is a test 1\nA\nB\nC\n" );
        assertEquals( srcs.get( "../src/foo.scm" ), "this is a test 2 B\nthis is a test 2\n" );
    }
}
