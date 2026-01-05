/**
 * Incremental generation support for proto-wrapper.
 *
 * <p>This package provides infrastructure for incremental code generation,
 * which only regenerates wrapper classes when their source proto files change.
 * This significantly reduces build times for large projects.
 *
 * <h2>Key Components</h2>
 *
 * <ul>
 *   <li>{@link space.alnovis.protowrapper.incremental.IncrementalStateManager} -
 *       Main coordinator for incremental generation</li>
 *   <li>{@link space.alnovis.protowrapper.incremental.IncrementalState} -
 *       Persistent state stored between builds</li>
 *   <li>{@link space.alnovis.protowrapper.incremental.FileFingerprint} -
 *       File content hash for change detection</li>
 *   <li>{@link space.alnovis.protowrapper.incremental.ChangeDetector} -
 *       Detects added, modified, and deleted proto files</li>
 *   <li>{@link space.alnovis.protowrapper.incremental.ProtoDependencyGraph} -
 *       Tracks import dependencies for transitive change detection</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 *
 * <ol>
 *   <li>On first build, all proto files are processed and state is saved</li>
 *   <li>On subsequent builds, proto files are fingerprinted (hash + timestamp)</li>
 *   <li>Changed files are detected by comparing fingerprints</li>
 *   <li>Dependency graph is used to find files that import changed files</li>
 *   <li>Only affected messages are regenerated</li>
 *   <li>New state is saved for next build</li>
 * </ol>
 *
 * <h2>Cache Invalidation</h2>
 *
 * <p>Full regeneration is triggered when:
 * <ul>
 *   <li>Plugin version changes</li>
 *   <li>Generation configuration changes</li>
 *   <li>Proto files are deleted</li>
 *   <li>Cache is manually cleared (clean build)</li>
 *   <li>Force flag is specified</li>
 * </ul>
 *
 * @since 1.6.0
 */
package space.alnovis.protowrapper.incremental;
