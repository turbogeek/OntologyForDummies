# OntologyForDummies
Ontology Vision is a set of ontology visualization tools that are specifically aimed at the novice (or slightly novice) ontologist. The basic hope is to allow the novice to read and create ontologies as well as give the expert ontologist a way to communicate with the muggles.

Unlike Harry (the) Potter, we think that ontologies are important to the mugggles (the owner of the project is a muggle who was creating an ontology on magic, so needed a way to learn the magic of ontology to create a magicc ontology). 

The first tool converts ontologies to reasonably easy to read ontologies. 

But there will be more! Why? AI first, because it is now easy for a gray-haired engineer/programmer to build cool tools. But also AI, because one of the multipliers of AI intellegence is good, standardized, nagvigatable, data. Ontologies meet this need. 

The problem is creating them as one of the biggest issues is that professional ontologists are some of the rarest brains on the planet. A quick Google search said a large ontology conference is 1,500 people. My first JavaOne conference was over 20,000. Google IO is 5,000 with thousands downloading the videos. Simply, to boost the creation of ontologies, we need more help. Yes, real ontologist need to be involved, but the precision, language, skills, and rules of ontology need a little translation for the muggles that are the experts in everything else - except ontology.

## Tool #1 Ontology to English
Our firt tool uses SBVR(you don't care what SBVR is yet, so I won't bore you) and the OWL API to create english-like translations of an ontology. It has been worked on over the years and Dassault Systems uses it in Cameo Concept Modeler(CCM) and other tools, but they never scratched the muggle brain cell itch. This tool creates a nice readable web page with colors and hyperlinks, with even more translations of the big words of ontology that sneak in. For many people, this simple translation will go quite far. If we find better, we will let you know.

## Build & Run (Tool #1: Ontology to English)
Requires a **Java 17+ JDK**. Everything else is fetched by the Gradle wrapper.

```bash
./gradlew build                                        # compile + run the tests
./gradlew run --args="src/test/resources/pizza.owl"    # writes pizza-sbvr.html next to the input
```

Or build one portable file you can run anywhere — the OWL API is bundled inside it:

```bash
./gradlew cliJar
java -jar build/libs/ontology-to-english-0.3.0-cli.jar examples/se-101.ttl --rosetta --open
```

See [`examples/`](examples/) for the bundled **Systems Engineering 101** ontology and a one-command fetch of the
real **Industrial Ontology Foundry** (IOF) ontologies to verbalize.

Options:

```
<input.owl|.ttl|.rdf> [output.html]
  --manchester                 OWL Manchester syntax instead of SBVR English
  --ose                        OWL Simplified English instead of SBVR English
  --rosetta                    side-by-side: SBVR | OSE | Manchester (a learning / "Rosetta" view)
  --validate                   run a reasoner (ELK): report consistency + unsatisfiable classes
  --color full|mono|plain      colored, color-blind-friendly mono, or plain text
  --no-model --no-owl --no-rdf --no-verbalization   leave out sections
  --no-rollover                drop the hover-the-word-to-see-its-meaning panel
  --title "My Ontology"
  --open                       open the result in your browser when done
```

Feed it any OWL file (RDF/XML, Turtle, OWL/XML, …). In **Cameo Concept Modeler** use *File ▸ Export Model to
OWL* first, then run the tool on the exported file — the same works for Protégé, TopBraid, or any editor that
exports OWL. There's also a Groovy version of the exact same thing in [`scripts/sbvr.groovy`](scripts/sbvr.groovy)
for tools that already have Groovy + the OWL API on the classpath.

### Use it from your own code
It's a small library too, with a single runtime dependency (the OWL API):

```java
OWLOntology ont = OWLManager.createOWLOntologyManager()
        .loadOntologyFromOntologyDocument(new File("pizza.owl"));
String html = new OntologyVerbalizer().verbalizeOntology(ont, "Pizza");   // full report
// or just the entities you care about:
String some = new OntologyVerbalizer().verbalizeEntities(ont, entities, "Selection");
```

## Help the Ontology Muggles
Do you want to help? Please do by writng issues on the project for new features and corrections. The aim is to integrate where we see gaps in specific tools and technologies too, like in Systems Engineering, Enterprise Architecture, AI, etc. 
