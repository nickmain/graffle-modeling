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

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.epistem.util.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple parser for OS X xml-encoded PLISTs.
 *
 * @author nickmain
 */
public class PListParser extends DefaultHandler {

    /**
     * The PLIST object - a map, list, byte[], Date, Double, Integer, String 
     * or Boolean.
     */
    public Object plistObject;

    private StringBuilder text = new StringBuilder();
    private Map<String,Object> dict;
    private List<Object> array;
    private String key;
    private LinkedList<Object> stack = new LinkedList<Object>();

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'" );
    
    private static final byte[] GZIP_SIG = { 0x1f, (byte) 0x8b, 0x08, 0x00 };
    
    //push current item onto the stack
    private void push() {
        if( dict != null ) {
            stack.addFirst( dict );
            stack.addFirst( key );
        }
        
        else if( array != null ) {
            stack.addFirst( array );
        }
        
        key   = null;
        dict  = null;
        array = null;
    }
    
    //pop item from the stack
    @SuppressWarnings("unchecked")
    private void pop() {
        key   = null;
        dict  = null;
        array = null;
        
        if( stack.isEmpty() ) return;
        
        try {
            Object obj = stack.removeFirst();
            
            if( obj instanceof String ) {
                key  = (String) obj;
                dict = (Map<String,Object>) stack.removeFirst();
            }
            else {
                array = (List<Object>) obj;
            }
        } catch( ClassCastException cce ) {
            System.err.println( cce );
        }
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        text.append( ch, start, length );
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        
        if( name.equals( "array" ) ) {
            plistObject = array;
            pop();
        }
        else if( name.equals( "data" ) ) {
            try{
                plistObject = Base64.decode( text.toString().trim() );
            } catch( Exception ex ) {
                throw new SAXException( "invalide base64 value" );
            }
        }
        else if( name.equals( "date" ) ) {
            try {
                plistObject = dateFormat.parse( text.toString().trim() );
            } catch( ParseException ex ) {
                throw new SAXException( ex );
            }
        }
        else if( name.equals( "dict" ) ) {
            plistObject = dict;
            pop();
        }
        else if( name.equals( "real" ) ) {
            plistObject = new Double( text.toString().trim() );
        }
        else if( name.equals( "integer" ) ) {
            plistObject = new Integer( text.toString().trim() );
        }
        else if( name.equals( "string" ) ) {
            plistObject = text.toString();
        }
        else if( name.equals( "key" ) ) {
            key = text.toString().trim();
            return;
        }
        else if( name.equals( "true" ) ) {
            plistObject = Boolean.TRUE;
        }
        else if( name.equals( "false" ) ) {
            plistObject = Boolean.FALSE;
        }

        //put the object in the current map
        if( key != null ) {
            dict.put( key, plistObject );
            key = null;
        }
        
        //put the object in the current array
        else if( array != null ) {
            array.add( plistObject );
        }
    }

    @Override
    public void startElement( String uri, String localName, String name, Attributes attributes ) 
        throws SAXException {
        
        text.setLength( 0 );
        
        if( name.equals( "array" ) ) {
            push();
            array = new ArrayList<Object>();
        }
        else if( name.equals( "dict" ) ) {
            push();
            dict = new HashMap<String, Object>();
        }
    }

    /**
     * Parse a PLIST file.
     * 
     * @param file the file to parse
     * @return the object contained in the plist - usually a Map or List
     */
    public static Object parse( File file ) throws Exception {
        if( file.isDirectory() ) file = new File( file, "data.plist" );

        SAXParserFactory fact = SAXParserFactory.newInstance();
        fact.setNamespaceAware( false );
        fact.setValidating( false );
        SAXParser parser = fact.newSAXParser();
        
        PListParser plist = new PListParser();
        
        //detect compressed file
        FileInputStream in = new FileInputStream( file );
        byte[] sig = new byte[4];
        in.read( sig );
        in.close();
        
        if( Arrays.equals( sig, GZIP_SIG ) ) {
            in = new FileInputStream( file );
            try {
                GZIPInputStream gzin = new GZIPInputStream( in );
                parser.parse( gzin, plist );
            }
            finally {
                in.close();
            }
        }        
        else {
            parser.parse( file, plist );
        }
        
        return plist.plistObject;
    }    
}
