package integration;

/** Shared hermetic fixtures for the ported q4j table contracts. */
abstract class TableContractTestSupport extends ITest {
  protected final void openTableModelContract(String page) {
    openFile("table-model-contract/" + page);
  }

  protected final void openClassicVariantsFixture() {
    openTableModelContract("classic.html");
  }

  protected final void openCustomGridsFixture() {
    openTableModelContract("custom-grids.html");
  }

  protected final void openEdgeCasesFixture() {
    openTableModelContract("edge-cases.html");
  }

  protected final void openQueriesFixture() {
    openTableModelContract("queries.html");
  }

  protected final void openAssertionsFixture() {
    openTableModelContract("assertions.html");
  }
}
