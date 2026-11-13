# Git workflow

Like a library: `main` is the shelf of finished books. You never write on the shelf; you
take a copy to your desk (a branch), write there, and only put it back once it's been
checked (a PR that builds).

## Branches

- `main` is protected by convention: it always builds and runs. No direct commits.
- Create one short-lived branch per change, from up-to-date `main`:

```sh
git switch main && git pull
git switch -c feat/herd-social-preference
```

| Prefix | Use for |
|---|---|
| `feat/` | New gameplay or feature |
| `fix/` | Bug fix |
| `refactor/` | Code change with no behavior change |
| `test/` | Tests only |
| `docs/` | Docs only |
| `chore/` | Build, dependencies, tooling, CI |

Names are lowercase-kebab-case and describe the change, not the person.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <imperative summary, max ~72 chars>

<optional body: why, not what>
```

- Types match branch prefixes: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, plus
  `perf` and `build`.
- Scopes: `core`, `herding`, `wellbeing`, `neoforge`, `build`, `docs`.
- Examples: `feat(herding): roll per-mob social preference on spawn`,
  `fix(wellbeing): clamp shelter score at 0`.
- Each commit builds (`./gradlew build`) and does one thing. Do not mix refactors with
  features.
- Never commit generated or local files: `build/`, `runs/`, `.gradle/`, IDE folders, Mojang
  sources. `.gitignore` covers these.

## Pull requests

1. Push the branch: `git push -u origin <branch>`.
2. Open a PR into `main` (`gh pr create`) using the template.
3. CI must pass. Review the diff yourself before merging.
4. **Squash merge**, so `main` has one Conventional Commit per PR. Delete the branch after.

## Versioning and releases

- Semantic versioning in `gradle.properties` (`mod_version`). `0.x` until the first public
  release.
- Tag releases on `main` as `v0.2.0` etc.

## For AI agents

- Work on a branch, never on `main`. Commit as you complete each logical step.
- Run `./gradlew build` before each commit.
- Push and open PRs only when the owner asks or the task says to.
- Never force-push `main`. Never rewrite history others have pulled.
