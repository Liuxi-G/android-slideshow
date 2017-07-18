package link.standen.michael.slideshow.model;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.File;

import link.standen.michael.slideshow.R;

/**
 * Model object for a file item.
 */
public class FileItem implements Comparable<FileItem> {

	private String name;
	private String path;
	private String pathUri;
	private Boolean isDirectory;
	private transient Bitmap thumbnail;
	/**
	 * At some point a thumbnail has been successfully generated.
	 */
	private Boolean hasThumbnail = Boolean.FALSE;
	/**
	 * At some point a thumbnail has been attempted (successfully or otherwise).
	 */
	private Boolean thumbnailAttempted = Boolean.FALSE;

	/**
	 * File passes the MIME type test.
	 */
	private Boolean isImage;

	private FileItemViewHolder holder;
	private boolean isSpecial = false;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
		this.pathUri = Uri.fromFile(new File(path)).toString();
	}

	public Boolean getIsDirectory() {
		return isDirectory;
	}

	public void setIsDirectory(Boolean directory) {
		isDirectory = directory;
	}

	@Override
	public int compareTo(@NonNull FileItem other) {
		if (this.getIsDirectory() == other.getIsDirectory()) {
			return this.getName().compareToIgnoreCase(other.getName());
		} else {
			return this.getIsDirectory() ? -1 : 1;
		}
	}

	@Override
	public boolean equals(Object other){
		return !(other == null || !(other instanceof FileItem)) &&
				this.getPath().equals(((FileItem)other).getPath());
	}

	public Bitmap getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Bitmap thumbnail) {
		this.thumbnail = thumbnail;
		if (thumbnail != null){
			hasThumbnail = true;
		}
		this.thumbnailAttempted = true;
		setHolderImageView();
	}

	public boolean couldHaveThumbnail(){
		return !(isDirectory || isSpecial);
	}

	/**
	 * Update the image view of the holder
	 */
	public void setHolderImageView(){
		if (holder != null && holder.getFileItem() == this){
			if (isSpecial) {
				if (isDirectory) {
					// Special Directory
					holder.getImageView().setImageResource(R.mipmap.special_folder);
				} else {
					// Special Play
					holder.getImageView().setImageResource(R.mipmap.play_folder);
				}
			} else if (isDirectory) {
				// Directory
				holder.getImageView().setImageResource(R.mipmap.folder);
			} else if (thumbnail != null){
				// Image thumb
				holder.getImageView().setImageBitmap(thumbnail);
			} else if (thumbnailAttempted && !hasThumbnail){
				// Something unknown
				holder.getImageView().setImageResource(R.mipmap.unknown);
			} else {
				// Loading
				holder.getImageView().setImageResource(R.mipmap.loading);
			}
		}
	}

	public FileItemViewHolder getHolder() {
		return holder;
	}

	public void setHolder(FileItemViewHolder holder) {
		this.holder = holder;
	}

	public Boolean getThumbnailAttempted() {
		return thumbnailAttempted;
	}

	public void setThumbnailAttempted(Boolean thumbnailAttempted) {
		this.thumbnailAttempted = thumbnailAttempted;
	}

	public Boolean getIsImage() {
		return isImage;
	}

	public void setIsImage(Boolean isImage) {
		this.isImage = isImage;
	}

	public Boolean getHasThumbnail() {
		return hasThumbnail;
	}

	public void setHasThumbnail(Boolean hasThumbnail) {
		this.hasThumbnail = hasThumbnail;
	}

	public boolean getIsSpecial() {
		return isSpecial;
	}

	public void setIsSpecial(boolean isSpecial) {
		this.isSpecial = isSpecial;
	}

	public String getPathUri() {
		return this.pathUri;
	}
}
