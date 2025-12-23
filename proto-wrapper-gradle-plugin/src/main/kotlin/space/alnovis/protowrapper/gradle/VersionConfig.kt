package space.alnovis.protowrapper.gradle

import org.gradle.api.Named
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Конфигурация версии protobuf схемы.
 * Используется в DSL:
 * ```
 * protoWrapper {
 *     versions {
 *         create("v1") {
 *             protoDir.set("proto/v1")
 *         }
 *         create("v2") {
 *             protoDir.set("proto/v2")
 *             versionName.set("V2Beta")
 *         }
 *     }
 * }
 * ```
 */
abstract class VersionConfig @Inject constructor(private val name: String) : Named {

    @Input
    override fun getName(): String = name

    /**
     * Директория с .proto файлами для этой версии (относительно protoRoot)
     */
    @get:Input
    abstract val protoDir: Property<String>

    /**
     * Кастомное имя версии (по умолчанию используется name с заглавной буквы)
     */
    @get:Input
    @get:Optional
    abstract val versionName: Property<String>

    /**
     * Список .proto файлов для исключения из обработки
     */
    @get:Input
    @get:Optional
    abstract val excludeProtos: ListProperty<String>

    /**
     * Get the effective version name for code generation.
     * Derived from versionName property or name with first letter uppercased.
     */
    @Internal
    fun getEffectiveName(): String = versionName.orNull ?: name.replaceFirstChar { it.uppercase() }

    /**
     * Get the version identifier in lowercase format.
     * Derived from effectiveName.
     */
    @Internal
    fun getVersionId(): String = getEffectiveName().lowercase()
}
