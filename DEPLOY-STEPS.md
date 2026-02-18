# Deploy to Maven Central – step-by-step

## 1. Prerequisites

- **Maven** installed and on PATH.
- **settings.xml** with OSSRH credentials at `C:\Users\<You>\.m2\settings.xml` (server id `ossrh`, username + password from Central Portal).
- **GPG (GnuPG)** — **free and open source** (no payment). Required by Maven Central to sign artifacts.
  - **Windows:** Install [Gpg4win](https://www.gpg4win.org/download.html) (choose “Simple install”). After install, **restart your terminal** (or IDE) so `gpg` is on PATH. Check: `gpg --version`.
  - **Create a key:** `gpg --full-generate-key` → choose default (RSA and RSA, 3072), enter your name/email, set a **passphrase** (you’ll type it when running `mvn deploy`).
  - **Publish the key (required for Central):** Sonatype validates signatures against public keyservers. Upload your public key **before** deploying:
    ```bash
    gpg --keyserver keys.openpgp.org --send-keys B770B0219107FF853D17C470C3D3F13C805A9DCB
    ```
    (Use your key fingerprint from `gpg --list-keys --keyid-format long`.) Wait a few minutes for propagation, then run `mvn deploy`.
  - If you get “Could not determine gpg version”: GnuPG is not installed or not on PATH; install Gpg4win and restart the terminal.

## 2. Deploy from your machine

Open a terminal in **CSTesting-Java** (same machine where `.m2/settings.xml` has the OSSRH credentials):

```bash
cd path\to\CSTesting-Java
mvn clean deploy
```

- Maven will build, run tests, **sign** the JAR (GPG will ask for your key passphrase), and **upload** to Sonatype’s staging repository.
- If something fails (e.g. “401 Unauthorized”), check `settings.xml` and that the server id is `ossrh`.
- **“Could not determine gpg version”** → Install [Gpg4win](https://www.gpg4win.org/download.html), then **restart your terminal** and run `gpg --version` to confirm. GPG is **free** (no payment).

**Without GPG (testing only):** You can skip signing so the build passes; upload may succeed but **release to Maven Central will fail** until artifacts are signed. Use only to test the rest of the pipeline:

```bash
mvn clean deploy -DskipSigning=true
```

To publish to Central you must install GPG and run `mvn clean deploy` (without `-DskipSigning=true`).

## 3. In the Central Portal / Nexus UI

1. **Refresh** the page (or open **Staging** / **Staging Repositories** if your UI has it).
2. Find the new staging repository (e.g. `iogithublokesh771988-xxxx`).
3. **Close** the staging repo (button in the UI). This validates the artifacts.
4. **Release** the staging repo. This publishes to Maven Central.
5. With `autoReleaseAfterClose: true` in the pom, release may happen automatically after close.

## 4. After release

- Artifacts appear on Maven Central after some time (often 10–30 minutes).
- Users can then depend on: `io.github.lokesh771988:cstesting-java:0.1.0`.
