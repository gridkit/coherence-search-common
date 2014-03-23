/**
 * Copyright 2011-2014 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.coherence.search.fastngram;

import java.util.Arrays;

/**
 * Helper class for dumping trees. Used for diagnostic
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class TextTree {

	public static TextTree t(String text, TextTree... children) {
		return new TextTree(text, children);
	}
	
	private String text;
	private TextTree[] children;
	
	public TextTree(String text, TextTree... children) {
		this.text = text;
		this.children = children;
	}
	
	public String printAsTree() {
		StringBuilder sb = new StringBuilder();
		printTreeNode(sb, "", this);
		return sb.toString();
	}

	private static void printTreeNode(StringBuilder sb, String prefix, TextTree node) {
		String t = String.valueOf(node.text);
		sb.append(t);
		if (node.children.length == 0) {
			sb.append('\n');
		}
		else {			
			String npref = prefix + blank(t.length()) + "| ";				
			String cpref = prefix + blank(t.length());
			if (node.children.length == 1) {
				sb.append("--");
				npref = cpref + "  ";
			}
			else {
				sb.append("+-");
			}
			printTreeNode(sb, npref , node.children[0]);
			for(int i = 1; i < node.children.length; ++i) {
				sb.append(cpref);
				if (i < node.children.length - 1) {
					sb.append("+-");
					printTreeNode(sb, npref , node.children[i]);
				}
				else {
					sb.append("\\-");
					printTreeNode(sb, cpref + "  " , node.children[i]);
				}
			}
		}
	}
	
	private static String blank(int n) {
		char[] ch = new char[n];
		Arrays.fill(ch, ' ');
		return new String(ch);
	}

	@Override
	public String toString() {
		return text + (children.length == 0 ? "" : Arrays.toString(children));
	}
}
