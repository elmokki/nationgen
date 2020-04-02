package nationGen.GUI;


import java.awt.*;
import java.util.function.Function;
import java.util.stream.IntStream;


/**
 * This is modified {@link GridLayout} where max widths and heights are kept track of per row/column.
 */
public class TableLayout implements LayoutManager2, java.io.Serializable {
	public enum Alignment {
		CENTER(0.5f, 0.5f),
		NORTH_WEST(0, 0),
		NORTH(0.5f, 0),
		NORTH_EAST(1, 0),
		EAST(1, 0.5f),
		SOUTH_EAST(1, 1),
		SOUTH(0.5f, 1),
		SOUTH_WEST(0, 1),
		WEST(0, 0.5f);
		
		final float x;
		final float y;
		Alignment(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
	
	/**
	 * This is the horizontal gap (in pixels) which specifies the space
	 * between columns.  They can be changed at any time.
	 * This should be a non-negative integer.
	 *
	 * @serial
	 * @see #getHgap()
	 * @see #setHgap(int)
	 */
	int hgap;
	/**
	 * This is the vertical gap (in pixels) which specifies the space
	 * between rows.  They can be changed at any time.
	 * This should be a non negative integer.
	 *
	 * @serial
	 * @see #getVgap()
	 * @see #setVgap(int)
	 */
	int vgap;
	/**
	 * This is the number of rows specified for the grid.  The number
	 * of rows can be changed at any time.
	 * This should be a non negative integer, where '0' means
	 * 'any number' meaning that the number of Rows in that
	 * dimension depends on the other dimension.
	 *
	 * @serial
	 * @see #getRows()
	 * @see #setRows(int)
	 */
	int rows;
	/**
	 * This is the number of columns specified for the grid.  The number
	 * of columns can be changed at any time.
	 * This should be a non negative integer, where '0' means
	 * 'any number' meaning that the number of Columns in that
	 * dimension depends on the other dimension.
	 *
	 * @serial
	 * @see #getColumns()
	 * @see #setColumns(int)
	 */
	int cols;
	
	Alignment alignment;
	
	/**
	 * Creates a table layout with a default of one column per component,
	 * in a single row.
	 * @since 1.1
	 */
	public TableLayout() {
		this(1, 0, 0, 0, Alignment.NORTH);
	}
	
	/**
	 * Creates a table layout with the specified number of rows and
	 * columns. All components in a row are given equal height and all
	 * components in a column are given equal width.
	 * <p>
	 * One, but not both, of {@code rows} and {@code cols} can
	 * be zero, which means that any number of objects can be placed in a
	 * row or in a column.
	 * @param     rows   the rows, with the value zero meaning
	 *                   any number of rows.
	 * @param     cols   the columns, with the value zero meaning
	 *                   any number of columns.
	 */
	public TableLayout(int rows, int cols) {
		this(rows, cols, 0, 0, Alignment.NORTH);
	}
	
	/**
	 * Creates a table layout with the specified number of rows and
	 * columns. All components in a row are given equal height and all
	 * components in a column are given equal width.
	 * <p>
	 * In addition, the horizontal and vertical gaps are set to the
	 * specified values. Horizontal gaps are placed between each
	 * of the columns. Vertical gaps are placed between each of
	 * the rows.
	 * <p>
	 * One, but not both, of {@code rows} and {@code cols} can
	 * be zero, which means that any number of objects can be placed in a
	 * row or in a column.
	 * <p>
	 * All {@code GridLayout} constructors defer to this one.
	 * @param     rows   the rows, with the value zero meaning
	 *                   any number of rows
	 * @param     cols   the columns, with the value zero meaning
	 *                   any number of columns
	 * @param     hgap   the horizontal gap
	 * @param     vgap   the vertical gap
	 * @exception   IllegalArgumentException  if the value of both
	 *                  {@code rows} and {@code cols} is
	 *                  set to zero
	 */
	public TableLayout(int rows, int cols, int hgap, int vgap, Alignment alignment) {
		if ((rows == 0) && (cols == 0)) {
			throw new IllegalArgumentException("rows and cols cannot both be zero");
		}
		this.rows = rows;
		this.cols = cols;
		this.hgap = hgap;
		this.vgap = vgap;
		this.alignment = alignment;
	}
	
	/**
	 * Gets the number of rows in this layout.
	 * @return    the number of rows in this layout
	 * @since     1.1
	 */
	public int getRows() {
		return rows;
	}
	
	/**
	 * Sets the number of rows in this layout to the specified value.
	 * @param        rows   the number of rows in this layout
	 * @exception    IllegalArgumentException  if the value of both
	 *               {@code rows} and {@code cols} is set to zero
	 * @since        1.1
	 */
	public void setRows(int rows) {
		if ((rows == 0) && (this.cols == 0)) {
			throw new IllegalArgumentException("rows and cols cannot both be zero");
		}
		this.rows = rows;
	}
	
	/**
	 * Gets the number of columns in this layout.
	 * @return     the number of columns in this layout
	 * @since      1.1
	 */
	public int getColumns() {
		return cols;
	}
	
	/**
	 * Sets the number of columns in this layout to the specified value.
	 * Setting the number of columns has no affect on the layout
	 * if the number of rows specified by a constructor or by
	 * the {@code setRows} method is non-zero. In that case, the number
	 * of columns displayed in the layout is determined by the total
	 * number of components and the number of rows specified.
	 * @param        cols   the number of columns in this layout
	 * @exception    IllegalArgumentException  if the value of both
	 *               {@code rows} and {@code cols} is set to zero
	 * @since        1.1
	 */
	public void setColumns(int cols) {
		if ((cols == 0) && (this.rows == 0)) {
			throw new IllegalArgumentException("rows and cols cannot both be zero");
		}
		this.cols = cols;
	}
	
	/**
	 * Gets the horizontal gap between components.
	 * @return       the horizontal gap between components
	 * @since        1.1
	 */
	public int getHgap() {
		return hgap;
	}
	
	/**
	 * Sets the horizontal gap between components to the specified value.
	 * @param        hgap   the horizontal gap between components
	 * @since        1.1
	 */
	public void setHgap(int hgap) {
		this.hgap = hgap;
	}
	
	/**
	 * Gets the vertical gap between components.
	 * @return       the vertical gap between components
	 * @since        1.1
	 */
	public int getVgap() {
		return vgap;
	}
	
	/**
	 * Sets the vertical gap between components to the specified value.
	 * @param         vgap  the vertical gap between components
	 * @since        1.1
	 */
	public void setVgap(int vgap) {
		this.vgap = vgap;
	}
	
	public Alignment getAlignment() {
		return this.alignment;
	}
	
	public void setAlignment(Alignment alignment) {
		this.alignment = alignment;
	}
	
	/**
	 * Adds the specified component with the specified name to the layout.
	 * @param name the name of the component
	 * @param comp the component to be added
	 */
	public void addLayoutComponent(String name, Component comp) {
	}
	
	@Override
	public void addLayoutComponent(Component comp, Object constraints) {
	}
	
	/**
	 * Removes the specified component from the layout.
	 * @param comp the component to be removed
	 */
	public void removeLayoutComponent(Component comp) {
	}
	
	public Dimension preferredLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			return new TableDimensions(parent, Component::getPreferredSize).getTotal();
		}
	}
	
	public Dimension minimumLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			return new TableDimensions(parent, Component::getMinimumSize).getTotal();
		}
	}
	
	public Dimension maximumLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			return new TableDimensions(parent, Component::getMaximumSize).getTotal();
		}
	}
	
	private class TableDimensions {
		final Insets insets;
		final int ncols;
		final int nrows;
		final int[] h;
		final int[] w;
		
		TableDimensions(Container parent, Function<Component, Dimension> whichSize) {
			insets = parent.getInsets();
			int ncomponents = parent.getComponentCount();
			
			if (rows > 0) {
				this.nrows = rows;
				this.ncols = (ncomponents + nrows - 1) / nrows;
			} else {
				this.ncols = cols;
				this.nrows = (ncomponents + ncols - 1) / ncols;
			}
			w = new int[ncols]; // current max width of each column
			h = new int[nrows]; // current max height of each row
			int i = 0;
			for (int row = 0; row < nrows; row++) {
				for (int col = 0; col < ncols; col++) {
					if (i >= ncomponents) {
						return;
					}
					Component comp = parent.getComponent(i++);
					Dimension d = whichSize.apply(comp);
					if (w[col] < d.width) {
						w[col] = d.width;
					}
					if (h[row] < d.height) {
						h[row] = d.height;
					}
				}
			}
		}
		
		Dimension getTotal() {
			return new Dimension(insets.left + insets.right + IntStream.of(w).sum() + (ncols-1)*hgap,
				insets.top + insets.bottom + IntStream.of(h).sum() + (nrows-1)*vgap);
		}
	}
	
	/**
	 * Lays out the specified container using this layout.
	 * <p>
	 * This method reshapes the components in the specified target
	 * container in order to satisfy the constraints of the
	 * {@code GridLayout} object.
	 * <p>
	 * The grid layout manager determines the size of individual
	 * components by dividing the free space in the container into
	 * equal-sized portions according to the number of rows and columns
	 * in the layout. The container's free space equals the container's
	 * size minus any insets and any specified horizontal or vertical
	 * gap. All components in a grid layout are given the same size.
	 *
	 * @param      parent   the container in which to do the layout
	 * @see        java.awt.Container
	 * @see        java.awt.Container#doLayout
	 */
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			TableDimensions d = new TableDimensions(parent, Component::getPreferredSize);
			
			int totalGapsWidth = (d.ncols - 1) * hgap;
			int widthWOInsets = parent.getWidth() - (d.insets.left + d.insets.right);
			int extraTotalWidthAvailable = widthWOInsets - (IntStream.of(d.w).sum() + totalGapsWidth);
			int extraLeftWidthAvailable = Math.round(extraTotalWidthAvailable * alignment.x);
			int extraRightWidthAvailable = extraTotalWidthAvailable - extraLeftWidthAvailable;
			
			int totalGapsHeight = (d.nrows - 1) * vgap;
			int heightWOInsets = parent.getHeight() - (d.insets.top + d.insets.bottom);
			int extraTotalHeightAvailable = heightWOInsets - (IntStream.of(d.h).sum() + totalGapsHeight);
			int extraTopHeightAvailable = Math.round(extraTotalHeightAvailable * alignment.y);
			
			int ncomponents = parent.getComponentCount();
			
			boolean ltr = parent.getComponentOrientation().isLeftToRight();
			if (ltr) {
				for (int c = 0, x = d.insets.left + extraLeftWidthAvailable; c < d.ncols; x += d.w[c] + hgap, c++) {
					for (int r = 0, y = d.insets.top + extraTopHeightAvailable; r < d.nrows; y += d.h[r] + vgap, r++) {
						int i = r * d.ncols + c;
						if (i < ncomponents) {
							parent.getComponent(i).setBounds(x, y, d.w[c], d.h[r]);
						}
					}
				}
			} else {
				for (int c = 0, x = (parent.getWidth() - d.insets.right - d.w[c]) - extraRightWidthAvailable; c < d.ncols ; c++, x -= d.w[c] + hgap) {
					for (int r = 0, y = d.insets.top + extraTopHeightAvailable; r < d.nrows ; y += d.h[r] + vgap, r++) {
						int i = r * d.ncols + c;
						if (i < ncomponents) {
							parent.getComponent(i).setBounds(x, y, d.w[c], d.h[r]);
						}
					}
				}
			}
		}
	}
	
	@Override
	public float getLayoutAlignmentX(Container target) {
		return this.alignment.x;
	}
	
	@Override
	public float getLayoutAlignmentY(Container target) {
		return this.alignment.y;
	}
	
	@Override
	public void invalidateLayout(Container target) {
	}
	
	/**
	 * Returns the string representation of this grid layout's values.
	 * @return     a string representation of this grid layout
	 */
	public String toString() {
		return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap +
			",rows=" + rows + ",cols=" + cols + ",alignment=" + alignment + "]";
	}
}
