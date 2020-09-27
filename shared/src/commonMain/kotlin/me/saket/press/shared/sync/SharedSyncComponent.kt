package me.saket.press.shared.sync

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.logging.SIMPLE
import kotlinx.serialization.json.Json
import me.saket.kgit.RealGit
import me.saket.press.shared.di.koin
import me.saket.press.shared.note.NoteFolder
import me.saket.press.shared.settings.Setting
import me.saket.press.shared.sync.git.GitHost
import me.saket.press.shared.sync.git.GitHostAuthToken
import me.saket.press.shared.sync.git.GitHostIntegrationPresenter
import me.saket.press.shared.sync.git.GitRepositoryCache
import me.saket.press.shared.sync.git.GitSyncer
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

class SharedSyncComponent {
  val module = module {
    single { httpClient(get()) }
    single { createJson() }

    factory {
      { host: GitHost ->
        Setting.create(
            settings = get(),
            key = "${host.name}_auth_token",
            from = ::GitHostAuthToken,
            to = GitHostAuthToken::value,
            defaultValue = null
        )
      }
    }
    single<GitRepositoryCache> { GitRepositoryCache.InMemory() }
    factory { SyncPreferencesPresenter(get(), get(), get(), get(), get(), get(), get()) }
    factory { (args: GitHostIntegrationPresenter.Args) ->
      GitHostIntegrationPresenter(
          args = args,
          httpClient = get(),
          authToken = get(),
          database = get(),
          cachedRepos = get(),
          syncCoordinator = get()
      )
    }

    single { SyncMergeConflicts() }
    single { (folder: NoteFolder?) ->
      GitSyncer(
          folder = folder,
          git = RealGit(),
          database = get(),
          deviceInfo = get(),
          clock = get(),
          strings = get(),
          mergeConflicts = get()
      )
    }
    single<SyncCoordinator> {
      RealSyncCoordinator(
          syncer = { folder -> koin<GitSyncer> { parametersOf(folder) } },
          schedulers = get()
      )
    }
  }

  private fun httpClient(json: Json): HttpClient {
    return HttpClient {
      install(JsonFeature) {
        serializer = KotlinxSerializer(json)
      }
      install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.INFO
      }
    }
  }

  companion object {
    fun preferencesPresenter(): SyncPreferencesPresenter = koin()

    fun integrationPresenter(args: GitHostIntegrationPresenter.Args) =
      koin<GitHostIntegrationPresenter> { parametersOf(args) }
  }
}

internal fun createJson(): Json {
  return Json(from = Json) {
    prettyPrint = true
    ignoreUnknownKeys = true
    useArrayPolymorphism = true
    isLenient = false
  }
}
