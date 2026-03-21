# 📸 Image Management Guide - Picasso + Local Storage

## Overview

Your Market Watch app now supports professional image management for user profiles and posts with these features:

✅ **Gallery Image Picker** - Users can select images from their device gallery  
✅ **Local Image Storage** - Images saved locally in app cache  
✅ **URL Loading** - Direct image URLs (web links)  
✅ **Automatic Caching** - Picasso handles caching automatically  
✅ **Image Optimization** - Automatic compression and resizing  
✅ **Circle Transforms** - Profile images displayed in circles  

---

## How It Works

### 1. Profile Picture Management

#### Location
`ProfileFragment.kt`

#### Features
- **Option 1**: Pick from Gallery
  - User selects image from device
  - Image saved locally to app storage
  - Automatically displayed as circular profile pic

- **Option 2**: Enter URL
  - User pastes direct web link (Imgur, Pinterest, etc.)
  - Image loaded and cached by Picasso

#### Code Example
```kotlin
// Load profile image (automatic handling of URL vs local file)
ImageManager.loadProfileImage(
    imageView = profileImageView,
    source = userProfilePictureUrl  // Can be URL or file path
)
```

### 2. Post Images

#### Location
`FeedFragment.kt`

#### Features
- Users can select images when creating/editing posts
- Images automatically saved locally
- Displayed in posts with optimization

#### Code Example
```kotlin
// When user picks image from gallery
val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
val savedPath = ImageManager.saveBitmapLocally(context, bitmap, "post_img.jpg")
```

---

## ImageManager Utility

### Available Methods

#### 1. Save Bitmap Locally
```kotlin
fun saveBitmapLocally(context: Context, bitmap: Bitmap, fileName: String?): String
```
**Purpose**: Save a bitmap to local storage  
**Returns**: Absolute file path  
**Quality**: 85% compression (optimized size)

#### 2. Load Image
```kotlin
fun loadImage(
    imageView: ImageView,
    source: String?,
    placeholderId: Int,
    errorId: Int,
    isCircle: Boolean
)
```
**Purpose**: Load image from URL or file path  
**Auto-detects**: URL vs local file  
**Handles**: Placeholder, error, resizing, caching

#### 3. Load Profile Image
```kotlin
fun loadProfileImage(
    imageView: ImageView,
    source: String?,
    placeholderId: Int
)
```
**Purpose**: Specialized for profile pictures  
**Features**: Circular transform applied automatically

#### 4. Cache Management
```kotlin
fun getImageCacheDir(context: Context): File
fun clearOldCache(context: Context)  // Clears images older than 30 days
fun clearAllCache(context: Context)  // Clears everything
fun getCacheSizeInMB(context: Context): Double
fun deleteCachedImage(filePath: String): Boolean
```

---

## Usage Examples

### Profile Picture

#### Option 1: Pick from Gallery
```kotlin
// User sees two options when clicking "Change Picture":
// 1. "Pick from Gallery" -> Opens gallery picker
// 2. "Enter Image URL" -> Shows URL dialog

// When user picks image:
val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
val savedPath = ImageManager.saveBitmapLocally(context, bitmap, "profile_pic.jpg")
viewModel.updateProfilePictureUrl(savedPath)
ImageManager.loadProfileImage(profileImageView, savedPath)
```

#### Option 2: URL
```kotlin
val url = "https://imgur.com/example.jpg"
viewModel.updateProfilePictureUrl(url)
ImageManager.loadProfileImage(profileImageView, url)
```

### Post Images

```kotlin
// User selects image for post
val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

// Save locally
val savedPath = ImageManager.saveBitmapLocally(
    context, 
    bitmap, 
    "post_img_${System.currentTimeMillis()}.jpg"
)

// Store path in post
viewModel.createPost(content = "My post", imageUri = Uri.fromFile(File(savedPath)))

// Display in adapter
ImageManager.loadImage(
    imageView = postImage,
    source = post.imageUrl,
    isCircle = false
)
```

---

## Storage Details

### Local Storage Locations
```
/data/data/com.example.marketwatch/files/
├── profile_pic.jpg         # Profile pictures
├── post_img_*.jpg          # Post images
└── (other local images)
```

### Cache Locations
```
/data/data/com.example.marketwatch/cache/images/
└── (Picasso manages this automatically)
```

### Storage Benefits
✅ **Fast**: No network delay  
✅ **Offline**: Images available without internet  
✅ **Optimized**: Automatically compressed  
✅ **Managed**: Automatic cleanup of old images  

---

## Configuration

### Image Quality
Currently set to **85% JPEG compression**
- Location: `ImageManager.kt` - `IMAGE_QUALITY` constant
- Balances quality vs file size

### Cache Duration
Currently set to **30 days**
- Location: `ImageManager.kt` - `CACHE_DURATION_DAYS` constant
- Older images auto-deleted

To modify:
```kotlin
private const val CACHE_DURATION_DAYS = 30L  // Change this
private const val IMAGE_QUALITY = 85         // Or this
```

---

## How Picasso Works

### Automatic Features
- **Caching**: Two-level caching (memory + disk)
- **Resizing**: Downscales to fit ImageView
- **Cropping**: Centers content
- **Thread Management**: Handles everything on background threads
- **Recycling**: Manages bitmap lifecycle

### Loading Flow
```
1. Check memory cache
   ↓ (not found)
2. Check disk cache
   ↓ (not found)
3. Load from source (URL or file)
4. Compress if needed
5. Store in caches
6. Display on UI thread
```

### Performance
- **First load**: Slight delay (fetch + cache)
- **Subsequent loads**: Instant (from cache)
- **No UI freezing**: All done on background threads

---

## Troubleshooting

### Image Not Showing?
1. Check file path is correct
2. Verify file exists in storage
3. Check ImageView size (must be > 0)
4. Try using placeholder: `R.drawable.ic_account_circle`

### Images Taking Too Long?
1. Check network speed (for URLs)
2. Clear cache: `ImageManager.clearAllCache(context)`
3. Reduce image quality if needed

### Storage Issues?
```kotlin
// Check cache size
val sizeInMB = ImageManager.getCacheSizeInMB(context)
Log.d("Images", "Cache size: ${sizeInMB}MB")

// Clear old images
ImageManager.clearOldCache(context)

// Clear everything
ImageManager.clearAllCache(context)
```

---

## Implementation Checklist

- [x] ImageManager utility class created
- [x] ProfileFragment updated with gallery picker
- [x] FeedFragment updated for post images
- [x] Local storage implemented
- [x] Picasso integration working
- [x] Cache management in place
- [x] Error handling added
- [x] Documentation provided

---

## Next Steps (Optional Improvements)

1. **Add image compression dialog**
   - Let user choose quality level

2. **Add image crop functionality**
   - Allow users to crop before saving

3. **Add image filters**
   - Brightness, contrast, etc.

4. **Add image upload to cloud**
   - Store in Firebase Storage instead of local

5. **Add image gallery view**
   - Show all saved images in a gallery

---

## Quick Reference

### For Profile Images
```kotlin
ImageManager.loadProfileImage(imageView, profilePictureUrl)
```

### For Post Images
```kotlin
ImageManager.loadImage(imageView, postImageUrl, isCircle = false)
```

### To Save Image
```kotlin
val path = ImageManager.saveBitmapLocally(context, bitmap, "filename.jpg")
```

### To Manage Cache
```kotlin
ImageManager.clearOldCache(context)              // Auto-cleanup
ImageManager.getCacheSizeInMB(context)           // Check size
ImageManager.clearAllCache(context)              // Full clear
```

---

## Summary

Your app now supports:
- ✅ Gallery image selection
- ✅ Local image storage
- ✅ URL image loading
- ✅ Automatic caching
- ✅ Image optimization
- ✅ Cache management

All handled professionally by Picasso and ImageManager! 🎉


