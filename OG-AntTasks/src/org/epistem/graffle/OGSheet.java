/*--------------------------------------------------------------------------------
  Copyright (c) 2010, David N. Main
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
package org.epistem.graffle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.epistem.graffle.OGUtils.*;

/**
 * An omnigraffle sheet
 *
 * @author nickmain
 */
@SuppressWarnings("unchecked")
public final class OGSheet {

    private final Map<String,Object> dict;
    private List<OGGraphic> oggraphics;
    
    /**
     * The layers
     */
    public final OGLayer[] layers;
    
    public final OmniGraffleDoc document;
    
    OGSheet( OmniGraffleDoc document, Map<String,Object> dict ) {
        this.dict = dict;
        this.document = document;
        
        List<Map<String,Object>> layerMaps = (List<Map<String,Object>>) dict.get( "Layers" );
        layers = new OGLayer[ layerMaps.size() ];
        for( int i = 0; i < layers.length; i++ ) {
            layers[i] = new OGLayer( layerMaps.get( i ) );
        }
    }

    /**
     * The sheet title
     */
    public String title() {
        return (String) dict.get( "SheetTitle" );
    }
    
    /**
     * Get the graphics
     */
    public List<OGGraphic> graphics() {
        if( oggraphics == null ) {
            oggraphics = new ArrayList<OGGraphic>();
            
            List<Object> graphics = (List<Object>) dict.get( "GraphicsList" );
            for( Object dict : graphics ) {
                oggraphics.add( new OGGraphic( this, null, (Map<String,Object>) dict ) );
            }
        }
        
        return oggraphics;
    }
    
    /**
     * Get the sheet's unique id
     */
    public int id() {
        return (Integer) dict.get( "UniqueID" );
    }
    
    /**
     * Get the sheet notes
     */
    public String notes() {
        Map<String,Object> bg = (Map<String,Object>) dict.get( "BackgroundGraphic" );
        String s = unRTF( (String) bg.get( "Notes" ));
        if( s == null ) return null;
        if( s.endsWith( "\n" ) ) s = s.substring( 0, s.length() - 1 );
        return s;
    }
    
    /**
     * Get the user defined properties
     */
    public Map<String,String> userProperties() {
        Map<String,Object> bg = (Map<String,Object>) dict.get( "BackgroundGraphic" );
        return (Map<String,String>) bg.get( "UserInfo" );
    }
}
