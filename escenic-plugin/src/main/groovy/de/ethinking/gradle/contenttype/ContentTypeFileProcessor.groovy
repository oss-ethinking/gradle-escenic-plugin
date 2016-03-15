/*
 *  Copyright 2015 eThinking GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.ethinking.gradle.contenttype

import groovy.xml.Namespace
import groovy.xml.QName
import groovy.xml.XmlUtil
import org.gradle.api.file.FileCollection

/**
 * Provides methods to merge and build content-types.xml file.
 */
class ContentTypeFileProcessor {

    static final Namespace CTP = new Namespace('http://ethinking.de/content-type-processing', 'ctp')
    static final Namespace UI = new Namespace('http://xmlns.escenic.com/2008/interface-hints', 'ui')

    /**
     * Inject multiple XML files into a base XML file.
     * @param basePath base XML file path
     * @param injectingPaths list of fragment file paths
     * @return XMLParser root node with the resulting XML tree
     */
    private Node injectInto(File baseFile, FileCollection injectingPaths) {
        Node baseRoot = new XmlParser(false, true).parse(baseFile)

        // put the injecting XMLs into the base
        injectingPaths.getFiles().each { File injectingPath ->
            Node injectingRoot = new XmlParser(false, true).parse(injectingPath)
            baseRoot.children().addAll(injectingRoot.children())
        }

        return baseRoot
    }

    /**
     * Processes custom attributes that specify reference of content-type to group.
     * @param root XMLParser node
     */
    private void processCustomAttributes(Node root) {

        Map<String, Node> groupMap = new HashMap<>()

        // make a map of groups
        root[(QName) UI.group].each { Node group ->
            groupMap.put((String) group.'@name', group)
        }

        root.'content-type'.each { Node contentType ->
            // get the specified group
            String uiGroup = contentType.attribute(CTP.'ui-group')
            if (uiGroup != null) {

                Node group = groupMap.get(uiGroup)
                if (group == null) {
                    throw new ContentTypeProcessingException("Specified UI group $uiGroup doesn't exist")
                }

                // create new node ref-content-type with attribute name from content type and add it to the group
                Node refContentType = new Node(group, UI.'ref-content-type')
                refContentType.'@name' = contentType.'@name'
                // remove the original attribute
                contentType.attributes().remove(CTP.'ui-group')
            }
        }
    }

    /**
     * Remove elements with a specified attribute, for which is the closure true.
     * @param root root where to find element tree
     * @param attributeName attribute which to search
     * @param closure close specifying whether to remove the value. It's only argument is the element's attribute's value.
     */
    private void removeElementWithAttribute(Node root, attributeName, Closure<Boolean> closure) {
        root.depthFirst().findAll { Node node ->
            node.attribute(attributeName) != null
        }.each { Node node ->
            String attributeValue = node.attribute(attributeName)
            // in case the current publication is NOT in the list of wanted, remove it
            if (closure.call(attributeValue)) {
                node.parent().remove(node)
            } else {
                // otherwise just remove the special attribute
                node.attributes().remove(attributeName)
            }
        }
    }

    /**
     * Processes custom attributes that specify publications in which they should be present.
     * @param root XMLParser node
     */
    private void processPublication(String publicationName, Node root) {
        // in case the current publication is NOT in the list of wanted, remove it
        removeElementWithAttribute(root, CTP.'in-publications') { String wantedPublications ->
            !Arrays.asList(wantedPublications.split(',')).contains(publicationName)
        }

        // when the current publication IS IN the list of unwanted, remove it
        removeElementWithAttribute(root, CTP.'not-in-publications') { String unwantedPublications ->
            Arrays.asList(unwantedPublications.split(',')).contains(publicationName)
        }
    }

    /**
     * Creates a content-type.xml file for a specified publication.
     * @param publicationName publication name
     * @param basePath path of the base config-type.xml
     * @param injectingPaths list of paths to inject into the base
     * @return created file content
     */
    String createFileForPublication(String publicationName, File baseFile, FileCollection fragments) {
        Node root;
        root = injectInto(baseFile, fragments)
        processCustomAttributes(root)
        processPublication(publicationName, root)
        XmlUtil.serialize(root)
    }

}
