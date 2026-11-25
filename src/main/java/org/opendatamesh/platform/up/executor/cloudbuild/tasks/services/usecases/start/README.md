# Start a build

Runs the Cloud Build trigger a DevOps task names, at the task's branch or tag, and returns the build resource name as the provider run id.

## Inputs and outputs

- In: `StartTaskCommand`, the trigger's name (`executorParameters.pipelineIdentifier`), the ref name and type, the pipeline parameters, and the service-account key header when the executor runs in service-account-key mode.
- Out: `StartTaskPresenter` receives the `BuildName` of the started build.
- The GCP project and location come from the executor configuration, through `StartTaskAccessOutboundPort`.

## Cases

- The trigger exists: it runs at the branch (`branchName`) or the tag (`tagName`) with the selected substitutions.
- No trigger has that name in the configured project and location: not found, and nothing runs.
- The task has no trigger name, no ref name, or a ref type other than BRANCH or TAG: invalid.
- In service-account-key mode the key header is missing: invalid, naming the header (`CredentialPolicy`).

## Rules

`StartTask` owns the procedure: trigger name, ref, credentials, trigger lookup, substitution selection, run.

`SubstitutionSelection` owns the substitution rules:

- `substitutionKey`: a parameter name becomes `_` plus the name in upper case, every character outside letters, digits and `_` replaced by `_`.
- `rejectCollisions`: two parameters that become the same key are invalid.
- `declaredByTrigger`: only keys the trigger declares are sent; the other parameters are dropped and their names logged. The trigger's default applies to a declared key with no parameter.
- `checkedKey` and `checkedValue`: a key longer than 100 bytes or a value longer than 4000 bytes is invalid.
- `rejectTooMany`: more than 200 substitutions is invalid.

## Tests

- `SubstitutionSelectionTest`
- `StartTaskTest`
- `StartTaskIT`
