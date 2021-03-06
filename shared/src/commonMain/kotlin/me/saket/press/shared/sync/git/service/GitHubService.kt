package me.saket.press.shared.sync.git.service

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.coroutinesinterop.completableFromCoroutine
import com.badoo.reaktive.coroutinesinterop.singleFromCoroutine
import com.badoo.reaktive.single.Single
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType.Application
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import me.saket.kgit.GitIdentity
import me.saket.press.shared.BuildKonfig
import me.saket.press.shared.sync.git.GitHostAuthToken
import me.saket.press.shared.sync.git.service.GitHostService.DeployKey

class GitHubService(private val http: HttpClient) : GitHostService {

  override fun generateAuthUrl(redirectUrl: String): String {
    return URLBuilder("https://github.com/login/oauth/authorize").apply {
      parameters.apply {
        append("client_id", BuildKonfig.GITHUB_CLIENT_ID)
        append("scope", "repo")
        append("redirect_uri", redirectUrl)
      }
    }.buildString()
  }

  override fun completeAuth(callbackUrl: String): Single<GitHostAuthToken> {
    return singleFromCoroutine {
      val response = http.post<GetAccessTokenResponse>("https://github.com/login/oauth/access_token") {
        contentType(Application.Json)
        body = GetAccessTokenRequest(
            client_id = BuildKonfig.GITHUB_CLIENT_ID,
            client_secret = BuildKonfig.GITHUB_CLIENT_SECRET,
            code = Url(callbackUrl).parameters["code"]!!
        )
      }
      GitHostAuthToken(response.access_token)
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  override fun fetchUserRepos(token: GitHostAuthToken): Single<List<GitRepositoryInfo>> {
    return singleFromCoroutine {
      var pageNum = 1
      var hasNextPage = true

      return@singleFromCoroutine buildList {
        while (hasNextPage) {
          val response = http.get<HttpResponse>("https://api.github.com/user/repos") {
            accept(Application.Json)
            header("Authorization", "token ${token.value}")
            parameter("per_page", "100")
            parameter("page", "${pageNum++}")
          }

          val responseBody = response.receive<List<GitHubRepo>>()
          addAll(responseBody.map {
            GitRepositoryInfo(
                owner = it.owner.login,
                name = it.name,
                url = it.html_url,
                sshUrl = it.ssh_url,
                defaultBranch = it.default_branch
            )
          })

          hasNextPage = "rel=\"next\"" in response.headers["Link"].orEmpty()
        }
      }
    }
  }

  override fun fetchUser(token: GitHostAuthToken): Single<GitIdentity> {
    return singleFromCoroutine {
      val response = http.get<GithubUserResponse>("https://api.github.com/user") {
        accept(Application.Json)
        header("Authorization", "token ${token.value}")
      }
      GitIdentity(name = response.login, email = response.email)
    }
  }

  override fun addDeployKey(token: GitHostAuthToken, repository: GitRepositoryInfo, key: DeployKey): Completable {
    return completableFromCoroutine {
      val response = http.post<String>("https://api.github.com/repos/${repository.owner}/${repository.name}/keys") {
        header("Authorization", "token ${token.value}")
        contentType(Application.Json)
        body = CreateDeployKeyRequest(
            title = key.title,
            key = key.key.publicKey,
            read_only = false
        )
      }
    }
  }
}

@Serializable
private data class GetAccessTokenRequest(
  val client_id: String,
  val client_secret: String,
  val code: String
)

@Serializable
private data class GetAccessTokenResponse(
  val access_token: String
)

@Serializable
private data class GitHubRepo(
  val name: String,
  val owner: Owner,
  val html_url: String,
  val ssh_url: String,
  val default_branch: String
) {
  @Serializable
  data class Owner(val login: String)
}

@Serializable
private data class GithubUserResponse(
  val login: String,
  val email: String?
)

@Serializable
private data class CreateDeployKeyRequest(
  val title: String,
  val key: String,
  val read_only: Boolean
)
