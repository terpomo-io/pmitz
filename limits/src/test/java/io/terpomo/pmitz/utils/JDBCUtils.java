/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.terpomo.pmitz.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class JDBCUtils {

	private static final String SCHEMA_PLACEHOLDER = "your_schema";
	private static final String COMMENT_PLACEHOLDER = "--";

	private JDBCUtils() {
	}

	public static void executeStatementsFile(Statement statement, String sqlFilePath, String schema) throws IOException, SQLException {

		List<String> lines = Files.readAllLines(Paths.get(sqlFilePath));
		List<String> sqlStatements = new ArrayList<>();

		StringBuilder currentStatement = new StringBuilder();
		boolean insideIfBlock = false;

		for (String line : lines) {
			line = line.trim();

			// Remove comments
			if (line.isEmpty() || line.startsWith(COMMENT_PLACEHOLDER)) {
				continue;
			}
			if (line.contains(COMMENT_PLACEHOLDER)) {
				line = line.substring(0, line.indexOf(COMMENT_PLACEHOLDER));
			}

			// Replace schema placeholder
			line = line.replace(SCHEMA_PLACEHOLDER, schema);

			// Append the line to the current statement
			currentStatement.append(line).append(" ");

			// Detect the beginning of an IF block
			if ("BEGIN".equalsIgnoreCase(line)) {
				insideIfBlock = true;
			}

			// Add the statement to the list if the IF block ends or if the line ends with ";"
			if (insideIfBlock && "END".equalsIgnoreCase(line)) {
				sqlStatements.add(currentStatement.toString());
				currentStatement.setLength(0);
				insideIfBlock = false;
			}
			else if (!insideIfBlock && line.endsWith(";")) {
				sqlStatements.add(currentStatement.toString());
				currentStatement.setLength(0);
			}
		}

		// Add last statement if present
		if (!currentStatement.isEmpty()) {
			sqlStatements.add(currentStatement.toString());
		}

		// Executing statements
		for (String sql : sqlStatements) {
			System.out.println("Executing SQL : " + sql);
			statement.execute(sql);
		}
	}
}
