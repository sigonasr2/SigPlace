<script type="text/javascript">
    function expand(modify,element) {
        var ele = document.getElementById("content_"+element);
        ele.classList.remove("content");
        ele.classList.add("expandedContent");
        modify.remove();
    }
</script>