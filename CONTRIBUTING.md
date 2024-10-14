<!-- omit in toc -->

# Contributing to Pmitz

First off, thanks for taking the time to contribute! ❤️

All types of contributions are encouraged and valued. See the [Table of Contents](#table-of-contents) for different ways
to help and details about how this project handles them. Please make sure to read the relevant section before making
your contribution. It will make it a lot easier for us maintainers and smooth out the experience for all involved. The
community looks forward to your contributions. 🎉

> And if you like the project, but just don't have time to contribute, that's fine. There are other easy ways to support
> the project and show your appreciation, which we would also be very happy about:
> - Star the project
> - Tweet about it
> - Refer this project in your project's readme
> - Mention the project at local meetups and tell your friends/colleagues

<!-- omit in toc -->

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [I Have a Question](#i-have-a-question)
- [I Want To Contribute](#i-want-to-contribute)
    - [Reporting Bugs](#reporting-bugs)
    - [Suggesting Enhancements](#suggesting-enhancements)
    - [Your First Code Contribution](#your-first-code-contribution)
    - [Improving The Documentation](#improving-the-documentation)
- [Styleguides](#styleguides)
    - [Source Code Style](#source-code-style)
    - [Commit Messages](#commit-messages)
- [Join The Project Team](#join-the-project-team)

## Code of Conduct

This project and everyone participating in it is governed by the
[Pmitz Code of Conduct](CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code. Please report unacceptable behavior
to <contact@terpomosoft.com>.

## I Have a Question

> If you want to ask a question, we assume that you have read the available [Documentation]().

Before you ask a question, it is best to search for existing [Issues](https://github.com/terpomo-io/pmitz/issues) that
might help you. In case you have found a suitable issue and still need clarification, you can write your question in
this issue. It is also advisable to search the internet for answers first.

If you then still feel the need to ask a question and need clarification, we recommend the following:

- Open an [Issue](https://github.com/terpomo-io/pmitz/issues/new).
- Provide as much context as you can about what you're running into.
- Provide project and platform versions (jvm, etc), depending on what seems relevant.

We will then take care of the issue as soon as possible.

<!--
You might want to create a separate issue tag for questions and include it in this description. People should then tag their issues accordingly.

Depending on how large the project is, you may want to outsource the questioning, e.g. to Stack Overflow or Gitter. You may add additional contact and information possibilities:
- IRC
- Slack
- Gitter
- Stack Overflow tag
- Blog
- FAQ
- Roadmap
- E-Mail List
- Forum
-->

## I Want To Contribute

> ### Legal Notice <!-- omit in toc -->
> When contributing to this project, you must agree that you have authored 100% of the content, that you have the
> necessary rights to the content and that the content you contribute may be provided under the project license.

### Reporting Bugs

<!-- omit in toc -->

#### Before Submitting a Bug Report

A good bug report shouldn't leave others needing to chase you up for more information. Therefore, we ask you to
investigate carefully, collect information and describe the issue in detail in your report. Please complete the
following steps in advance to help us fix any potential bug as fast as possible.

- Make sure that you are using the latest version.
- Determine if your bug is really a bug and not an error on your side e.g. using incompatible environment
  components/versions (Make sure that you have read the [documentation](). If you are looking for support, you might
  want to check [this section](#i-have-a-question)).
- To see if other users have experienced (and potentially already solved) the same issue you are having, check if there
  is not already a bug report existing for your bug or error in
  the [bug tracker](https://github.com/terpomo-io/pmitz/issues?q=label%3Abug).
- Also make sure to search the internet (including Stack Overflow) to see if users outside of the GitHub community have
  discussed the issue.
- Collect information about the bug:
    - Stack trace (Traceback)
    - OS, Platform and Version (Windows, Linux, macOS, x86, ARM)
    - Version of the interpreter, compiler, SDK, runtime environment, package manager, depending on what seems relevant.
    - Possibly your input and the output
    - Can you reliably reproduce the issue? And can you also reproduce it with older versions?

<!-- omit in toc -->

#### How Do I Submit a Good Bug Report?

> You must never report security related issues, vulnerabilities or bugs including sensitive information to the issue
> tracker, or elsewhere in public. Instead sensitive bugs must be sent by email to <contact@terpomosoft.com>.
<!-- You may add a PGP key to allow the messages to be sent encrypted as well. -->

We use GitHub issues to track bugs and errors. If you run into an issue with the project:

- Open an [Issue](https://github.com/terpomo-io/pmitz/issues/new). (Since we can't be sure at this point whether it is a
  bug or not, we ask you not to talk about a bug yet and not to label the issue.)
- Explain the behavior you would expect and the actual behavior.
- Please provide as much context as possible and describe the *reproduction steps* that someone else can follow to
  recreate the issue on their own. This usually includes your code. For good bug reports you should isolate the problem
  and create a reduced test case.
- Provide the information you collected in the previous section.

Once it's filed:

- The project team will label the issue accordingly.
- A team member will try to reproduce the issue with your provided steps. If there are no reproduction steps or no
  obvious way to reproduce the issue, the team will ask you for those steps and mark the issue as `needs-repro`. Bugs
  with the `needs-repro` tag will not be addressed until they are reproduced.
- If the team is able to reproduce the issue, it will be marked `needs-fix`, as well as possibly other tags (such
  as `critical`), and the issue will be left to be [implemented by someone](#your-first-code-contribution).

<!-- You might want to create an issue template for bugs and errors that can be used as a guide and that defines the structure of the information to be included. If you do so, reference it here in the description. -->

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for Pmitz, **including completely new features and
minor improvements to existing functionality**. Following these guidelines will help maintainers and the community to
understand your suggestion and find related suggestions.

<!-- omit in toc -->

#### Before Submitting an Enhancement

- Make sure that you are using the latest version.
- Read the [documentation]() carefully and find out if the functionality is already covered, maybe by an individual
  configuration.
- Perform a [search](https://github.com/terpomo-io/pmitz/issues) to see if the enhancement has already been suggested.
  If it has, add a comment to the existing issue instead of opening a new one.
- Find out whether your idea fits with the scope and aims of the project. It's up to you to make a strong case to
  convince the project's developers of the merits of this feature. Keep in mind that we want features that will be
  useful to the majority of our users and not just a small subset. If you're just targeting a minority of users,
  consider writing an add-on/plugin library.

<!-- omit in toc -->

#### How Do I Submit a Good Enhancement Suggestion?

Enhancement suggestions are tracked as [GitHub issues](https://github.com/terpomo-io/pmitz/issues).

- Use a **clear and descriptive title** for the issue to identify the suggestion.
- Provide a **step-by-step description of the suggested enhancement** in as many details as possible.
- **Describe the current behavior** and **explain which behavior you expected to see instead** and why. At this point
  you can also tell which alternatives do not work for you.
- You may want to **include screenshots and animated GIFs** which help you demonstrate the steps or point out the part
  which the suggestion is related to. You can use [this tool](https://www.cockos.com/licecap/) to record GIFs on macOS
  and Windows, and [this tool](https://github.com/colinkeenan/silentcast)
  or [this tool](https://github.com/GNOME/byzanz) on
  Linux. <!-- this should only be included if the project has a GUI -->
- **Explain why this enhancement would be useful** to most Pmitz users. You may also want to point out the other
  projects that solved it better and which could serve as inspiration.

<!-- You might want to create an issue template for enhancement suggestions that can be used as a guide and that defines the structure of the information to be included. If you do so, reference it here in the description. -->

### Your First Code Contribution

We are excited to help you make your first code contribution! Follow these steps to set up your environment, configure your IDE, and get started with your first contribution.

#### Setting Up Your Environment

1. Install Java Development Kit (JDK):
    * Download and install the version of the JDK (Java 17 or higher).
    * Verify the installation by running ```java -version``` in your terminal.

1. Clone the Repository:
    * Fork the repository on GitHub.
    * Clone your forked repository to your local machine:
      ```
      git clone https://github.com/terpomo-io/pmitz.git
      cd pmitz
      ```

#### Configuring IntelliJ IDEA

1. Download and Install IntelliJ IDEA:
    * Download IntelliJ IDEA from the JetBrains website.
    * Install and launch IntelliJ IDEA.

1. Import the Project:
    * Open IntelliJ IDEA and select File > New > Project from Existing Sources.
    * Navigate to the cloned repository directory and select it.
    * Follow the prompts to import the project as a Gradle project.

1. Configure the JDK:
    * Go to File > Project Structure > Project.
    * Set the Project SDK to the JDK you installed earlier.

1. Import Code Style Settings:
    * Our project includes a code style configuration file to ensure consistent formatting.
    * Go to File > Settings > Editor > Code Style.
    * Click on the gear icon and select Import Scheme > IntelliJ IDEA code style XML.
    * Navigate to the code style file in the project directory (```./checkstyle/Pmitz_Code_Style.xml```) and import it.

#### Getting Started

1. Build the Project:
    * Open the terminal in IntelliJ IDEA and run the following command to build the project:
      ```
      ./gradlew build
      ```
    * Ensure that the build completes successfully without errors.

1. Run the Example Application:
    * Our repository includes an example application to help you learn how to use the product.
    * Locate the main class of the example application (```./examples```).
    * Right-click on the main class and select Run.

3. Explore the Codebase:
    * Familiarize yourself with the project structure and codebase.

#### Making Your First Contribution

1. Create a New Branch:
    * Create a new branch for your contribution:
      ```
      git checkout -b your-branch-name
      ```

1. Make Your Changes:
    * Implement your changes in the codebase.
    * Ensure your code follows the project’s coding standards and guidelines.

1. Commit and Push Your Changes:
    * Commit your changes with a meaningful commit message:
      ```
      git commit -m "Description of your changes"
      ```

    * Push your changes to your forked repository:
      ```
      git push origin your-branch-name
      ```
1. Submit a Pull Request:
    * Go to the original repository on GitHub and open a pull request.
    * Provide a clear description of your changes and link to any relevant issues.

#### Need Help?
If you encounter any issues or need assistance, feel free to reach out to us via <contact@terpomosoft.com>. We are here to support you and look forward to your contributions!

### Improving The Documentation

Clear and comprehensive documentation is crucial for the success of our project. We welcome contributions to improve our documentation, making it easier for everyone to understand and use our project. Here’s how you can help:

#### How to Contribute
Identify Areas for Improvement: Review the existing documentation and identify sections that need clarification, expansion, or updating.
Propose Changes: Open an issue in our issue tracker to propose your changes. Describe what you plan to improve and why it’s necessary.
Make Edits: Fork the repository and make your edits. Ensure your changes are clear, concise, and follow our documentation style guide.
Submit a Pull Request: Once your edits are complete, submit a pull request. Be sure to link it to the relevant issue and provide a summary of your changes.

#### Documentation Guidelines
Clarity: Write in a clear and straightforward manner. Avoid jargon and explain technical terms.
Consistency: Follow the established style and formatting guidelines to maintain consistency across the documentation.
Accuracy: Ensure all information is accurate and up-to-date. Verify any technical details with the codebase or relevant contributors.
Examples: Include examples and code snippets where applicable to illustrate concepts and usage.

#### Get in Touch
If you have any questions or need assistance, feel free to reach out to us via <contact@terpomosoft.com>. We appreciate your efforts to improve our documentation and make our project more accessible to everyone!

## Styleguides

### Source Code Style

Coding convention rules are described in our [code style page](CODE_STYLE.md).

To configure source formatting according to Pmitz style rules, you can import the 
`Pmitz_Code_style.xml` configuration file into IntelliJ IDEA (**`Settings`**/**`Editor`**/**`Code Style`**/**`Java`**).

### Commit Messages

Guidelines are to follow on the content of the message when committing Git. This allows for more readable and
easy-to-follow messages when browsing the project's change history. Additionally, we use these messages to generate the
changelog.
Please follow these guidelines which are taken from Chris Beams blog
post: [How to Write a Git Commit Message](https://cbea.ms/git-commit/).

## Join The Project Team

<!-- TODO -->

<!-- omit in toc -->

## Attribution

This guide is based on the **contributing-gen**. [Make your own](https://github.com/bttger/contributing-gen)!
