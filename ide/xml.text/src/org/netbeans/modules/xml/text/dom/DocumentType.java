/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.netbeans.modules.xml.text.dom;

import org.netbeans.modules.xml.text.api.dom.XMLSyntaxSupport;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.xml.lexer.XMLTokenId;
import org.netbeans.modules.xml.spi.dom.*;

/**
 * It should envolve in DocumentType implementation.
 *
 * @author Petr Kuzel
 */
public class DocumentType extends SyntaxNode implements org.w3c.dom.DocumentType {

    public DocumentType(XMLSyntaxSupport syntax, Token<XMLTokenId> first, int start, int end) {
        super (syntax, first, start, end);
    }

    public short getNodeType() {
        return org.w3c.dom.Node.DOCUMENT_TYPE_NODE;
    }
        
    public String getPublicId() {
//        TokenItem first = first();
//        String doctype = first.getImage();
//        if (doctype.indexOf("PUBLIC") != -1) {                                  // NOI18N
//            TokenItem next = first.getNext();
//            if (next != null && next.getTokenID() == VALUE) {
//                String publicId = next.getImage();
//                return publicId.substring(1, publicId.length() - 1);
//            }
//        }
        return null;
    }
    
    public org.w3c.dom.NamedNodeMap getNotations() {
        return NamedNodeMapImpl.EMPTY;
    }
    
    public String getName() {
//        //<!DOCTYPE id ...
//        String docType = first().getImage();
//        int idIndex = docType.indexOf(' ');
//        if(idIndex > 0) {
//            int idEndIndex = docType.indexOf(' ', idIndex + 1);
//            if(idEndIndex > 0 && idEndIndex > idIndex) {
//                return docType.substring(idIndex + 1, idEndIndex);
//            }
//        }
        return null;
    }
    
    public org.w3c.dom.NamedNodeMap getEntities() {
        return NamedNodeMapImpl.EMPTY;
    }
    
    public String getSystemId() {
//        TokenItem first = first();
//        String doctype = first.getImage();
//        if (doctype.indexOf("PUBLIC") != -1) {                                  // NOI18N
//            TokenItem next = first.getNext();
//            if (next != null && next.getTokenID() == VALUE) {
//                next = next.getNext();
//                if (next == null) return null;
//                next = next.getNext();
//                if (next != null && next.getTokenID() == VALUE) {
//                    String system = next.getImage();
//                    return system.substring(1, system.length() -1);
//                }
//            }
//        } else if (doctype.indexOf("SYSTEM") != -1) {                           // NOI18N
//            TokenItem next = first.getNext();
//            if (next != null && next.getTokenID() == VALUE) {
//                String system = next.getImage();
//                return system.substring(1, system.length() - 1);
//            }
//        }
        return null;
    }
    
    public String getInternalSubset() {
        return null;
    }
    
}

