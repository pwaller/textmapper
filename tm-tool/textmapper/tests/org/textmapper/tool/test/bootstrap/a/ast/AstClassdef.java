/**
 * Copyright 2002-2013 Evgeny Gryaznov
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
package org.textmapper.tool.test.bootstrap.a.ast;

import java.util.List;
import org.textmapper.tool.test.bootstrap.a.SampleATree.TextSource;

public class AstClassdef extends AstNode implements IAstClassdefNoEoi {

	private String identifier;
	private List<AstClassdeflistItem> classdeflist;

	public AstClassdef(String identifier, List<AstClassdeflistItem> classdeflist, TextSource input, int start, int end) {
		super(input, start, end);
		this.identifier = identifier;
		this.classdeflist = classdeflist;
	}

	public String getIdentifier() {
		return identifier;
	}
	public List<AstClassdeflistItem> getClassdeflist() {
		return classdeflist;
	}
	public void accept(AstVisitor v) {
		if (!v.visit(this)) {
			return;
		}

		if (classdeflist != null) {
			for (AstClassdeflistItem it : classdeflist) {
				it.accept(v);
			}
		}
	}
}