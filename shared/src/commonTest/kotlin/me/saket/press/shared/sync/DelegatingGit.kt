package me.saket.press.shared.sync

import me.saket.kgit.Git
import me.saket.kgit.GitConfig
import me.saket.kgit.GitPullResult
import me.saket.kgit.GitRepository
import me.saket.kgit.PushResult
import me.saket.kgit.SshPrivateKey
import me.saket.kgit.UtcTimestamp

class DelegatingGit(private val delegate: Git) : Git {
  var prePull: (() -> Unit)? = null
  var preCommit: ((message: String) -> Unit)? = null
  var prePush: (() -> Unit)? = null
  var pushCount = 0

  override fun repository(path: String, sshKey: SshPrivateKey, remoteSshUrl: String, userConfig: GitConfig) =
    DelegatingGitRepository(this, delegate.repository(path, sshKey, remoteSshUrl, userConfig))

  class DelegatingGitRepository(
    private val git: DelegatingGit,
    private val delegate: GitRepository
  ) : GitRepository by delegate {

    override fun pull(rebase: Boolean): GitPullResult {
      git.prePull?.invoke()
      return delegate.pull(rebase)
    }

    override fun commitAll(message: String, timestamp: UtcTimestamp, allowEmpty: Boolean) {
      git.preCommit?.invoke(message)
      delegate.commitAll(message, timestamp, allowEmpty)
    }

    override fun push(force: Boolean): PushResult {
      git.pushCount++
      git.prePush?.invoke()
      return delegate.push(force)
    }
  }
}
