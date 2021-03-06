package press.theme

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.OnHierarchyChangeListener
import android.view.Window.ID_ANDROID_CONTENT
import android.widget.Button
import android.widget.EdgeEffect
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.saket.press.R
import press.extensions.findTitleView
import press.extensions.textColor
import press.widgets.PorterDuffColorFilterWrapper
import press.widgets.PressButton
import press.widgets.ScrollViewCompat

/**
 * Registers theme change listeners on each Views of a View hierarchy so
 * that the app's theme can change without requiring a restart.
 */
object AutoThemer {
  fun theme(activity: Activity) {
    themeGroup(activity.findViewById(ID_ANDROID_CONTENT))
  }

  private fun themeGroup(viewGroup: ViewGroup) {
    viewGroup.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
      override fun onChildViewAdded(parent: View, child: View) {
        when (child) {
          is ViewGroup -> themeGroup(child)
          else -> themeView(child)
        }
      }

      override fun onChildViewRemoved(parent: View, child: View) = Unit
    })

    themeView(viewGroup)
    viewGroup.children.forEach { child ->
      when (child) {
        is ViewGroup -> themeGroup(child)
        else -> themeView(child)
      }
    }
  }

  private fun themeView(view: View) {
    // Views can get recycled. Avoid theming them again.
    if (view.getTag(R.id.theming_done) as? Boolean == true) return
    view.setTag(R.id.theming_done, true)

    when (view) {
      is EditText -> themed(view)
      is Button -> themed(view)
      is TextView -> themed(view)
      is ScrollView -> themed(view)
      is RecyclerView -> themed(view)
      is Toolbar -> themed(view)
      is FloatingActionButton -> themed(view)
      is ProgressBar -> themed(view)
    }
  }
}

private fun <T : TextView> themed(view: T): T = view.apply {
  themeAware {
    setLinkTextColor(it.accentColor)
    highlightColor = it.textHighlightColor
  }
}

private fun <T : Button> themed(view: T): T = view.apply {
  check(view is PressButton) { "Use PressButton instead" }
}

private fun <T : EditText> themed(view: T): T = view.apply {
  require(view !is AppCompatEditText) { "Cursor tinting doesn't work with AppCompatEditText, not sure why." }
  val selectionHandleDrawables = TextViewCompat.textSelectionHandles(this)

  themeAware { palette ->
    selectionHandleDrawables.forEach { it.setColorFilter(palette.accentColor, SRC_IN) }
    highlightColor = palette.textHighlightColor
    setHintTextColor(palette.textColorSecondary)
  }
}

private fun themed(view: ScrollView) = view.apply {
  themeAware {
    ScrollViewCompat.setEdgeEffectColor(view, it.accentColor)
  }
}

private fun <T : RecyclerView> themed(view: T): T = view.apply {
  themeAware {
    edgeEffectFactory = object : EdgeEffectFactory() {
      override fun createEdgeEffect(view: RecyclerView, direction: Int) =
        EdgeEffect(view.context).apply { color = it.accentColor }
    }
  }
}

private fun themed(toolbar: Toolbar) = toolbar.apply {
  val titleView = findTitleView()
  titleView.typeface = ResourcesCompat.getFont(context, R.font.work_sans_bold)

  themeAware {
    titleView.textColor = it.textColorHeading
  }
}

private fun themed(view: FloatingActionButton) = view.apply {
  themeAware {
    backgroundTintList = ColorStateList.valueOf(it.fabColor)
    colorFilter = PorterDuffColorFilterWrapper(it.fabIcon)
  }
}

private fun <T : ProgressBar> themed(view: T): T = view.apply {
  themeAware {
    view.indeterminateTintList = ColorStateList.valueOf(it.accentColor)
  }
}
