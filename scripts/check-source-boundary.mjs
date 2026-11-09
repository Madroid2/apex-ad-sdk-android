import { execFileSync } from "node:child_process";

const files = execFileSync("git", ["ls-files", "-z"], { encoding: "utf8" }).split("\0");
const prohibited = files.filter((path) =>
  path.startsWith("site/") || path === ".github/workflows/pages.yml"
);

if (prohibited.length) {
  console.error("Website source and deployment must remain in the private website repository.");
  console.error(prohibited.join("\n"));
  process.exitCode = 1;
} else {
  console.log("SDK source boundary verified: no website files or Pages deployment workflow.");
}
