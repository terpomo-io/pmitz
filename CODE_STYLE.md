# Code style

The coding convention required in the contributions to the project is that used by
the [Spring Code Style](https://github.com/spring-projects/spring-framework/wiki/Code-Style). Only the few following
points modify it.

### License

Each source file must specify the following license at the very top of the file :

```java
/*
 * Copyright 2023-2026 the original author or authors.
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
 ```

### Changed CheckStyle module configuration

#### `JavadocPackageCheck`

CheckStyle's `JavadocPackageCheck` module checks that each Java package has a `package-info.java` file used for comments.
This check has been disabled.

#### `RequireThisCheck`

CheckStyle's `RequireThisCheck` module ensures that references to instance variables and methods of the current object 
are explicitly in the form this.VariableName or this.MethodeName(args). It checks that these references do not depend 
on the default behavior when `this.` is absent.
This check has been disabled for instance variables.

#### `JavadocVariableCheck`

The CheckStyle `JavadocVariableCheck` module checks that a variable has a Javadoc comment.
This check has been disabled.


